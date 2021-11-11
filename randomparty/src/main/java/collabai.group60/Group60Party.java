package collabai.group60;

import java.io.IOException;
import java.math.BigInteger;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

import geniusweb.actions.Action;
import geniusweb.actions.LearningDone;
import geniusweb.actions.Offer;
import geniusweb.actions.PartyId;
import geniusweb.actions.Vote;
import geniusweb.actions.VoteWithValue;
import geniusweb.actions.Votes;
import geniusweb.actions.VotesWithValue;
import geniusweb.bidspace.AllPartialBidsList;
//import geniusweb.bidspace.pareto.ParetoLinearAdditive;
import geniusweb.inform.ActionDone;
import geniusweb.inform.Finished;
import geniusweb.inform.Inform;
import geniusweb.inform.OptIn;
import geniusweb.inform.OptInWithValue;
import geniusweb.inform.Settings;
import geniusweb.inform.Voting;
import geniusweb.inform.YourTurn;
import geniusweb.issuevalue.Bid;
import geniusweb.issuevalue.DiscreteValue;
import geniusweb.issuevalue.NumberValue;
import geniusweb.issuevalue.Value;
import geniusweb.opponentmodel.FrequencyOpponentModel;
import geniusweb.party.Capabilities;
import geniusweb.party.DefaultParty;
import geniusweb.profile.PartialOrdering;
import geniusweb.profile.Profile;
//import geniusweb.profile.utilityspace.LinearAdditive;
import geniusweb.profile.utilityspace.UtilitySpace;
import geniusweb.profileconnection.ProfileConnectionFactory;
import geniusweb.profileconnection.ProfileInterface;
import geniusweb.progress.Progress;
import geniusweb.progress.ProgressRounds;
import tudelft.utilities.logging.Reporter;

/**
 * A party that places random bids and accepts when it receives an offer with
 * sufficient utility. This party is also a demo on how to support the various
 * protocols, which causes this party to look a bit complex.
 *
 * <h2>parameters</h2>
 * <table >
 * <caption>parameters</caption>
 * <tr>
 * <td>minPower</td>
 * <td>This value is used as minPower for placed {@link Vote}s. Default value is
 * 2.</td>
 * </tr>
 * <tr>
 * <td>maxPower</td>
 * <td>This value is used as maxPower for placed {@link Vote}s. Default value is
 * infinity.</td>
 * </tr>
 * </table>
 */
public class Group60Party extends DefaultParty {

	private Bid lastReceivedBid = null;
	private PartyId me;
	private final Random random = new Random();
	protected ProfileInterface profileint = null;
	private Progress progress;
	private Settings settings;
	private Votes lastvotes;
	private VotesWithValue lastvoteswithvalue;
	private String protocol;
	private Integer Pmax = Integer.MAX_VALUE;
	private Map<PartyId, Integer> oppPowers;
	private Map<PartyId, FrequencyOpponentModel> opponentModelMap;
	private Set<Bid> mostRecentOptimalPoints = null;
	private boolean firstBid = true; //this is true if we're just starting the session. Can be used to initialize array sizes etc.
	protected double reservationValue;

	public Group60Party() {
	}

	public Group60Party(Reporter reporter) {
		super(reporter); // for debugging
	}

	@Override
	public void notifyChange(Inform info) {
		try {
			if (info instanceof Settings) {
				Settings sett = (Settings) info;
				this.me = sett.getID();
				this.progress = sett.getProgress();
				this.settings = sett;
				this.protocol = sett.getProtocol().getURI().getPath();
				Object val = sett.getParameters().get("reservationValue");
				this.reservationValue = (val instanceof Double) ? (Double) val : 0.7;
				if ("Learn".equals(protocol)) {
					getConnection().send(new LearningDone(me));
				} else {
					this.profileint = ProfileConnectionFactory
							.create(sett.getProfile().getURI(), getReporter());
				}
				try {
					this.reservationValue = ((UtilitySpace) this.profileint.getProfile()).getUtility(this.profileint.getProfile().getReservationBid()).doubleValue();
				} catch (Exception e) {
					getReporter().log(Level.WARNING, "No reservation bid found");
				}
			} else if (info instanceof ActionDone) {
				Action otheract = ((ActionDone) info).getAction();
				if (otheract instanceof Offer) {
					lastReceivedBid = ((Offer) otheract).getBid();
				}
			} else if (info instanceof YourTurn) {
				makeOffer();
			} else if (info instanceof Finished) {
				getReporter().log(Level.INFO, "Final ourcome:" + info);
				terminate(); // stop this party and free resources.
			} else if (info instanceof Voting) {
				switch (protocol) {
					case "MOPAC":
						lastvotes = vote((Voting) info);
						getConnection().send(lastvotes);
						break;
					case "MOPAC2":
						lastvoteswithvalue = voteWithValue((Voting) info);
						getConnection().send(lastvoteswithvalue);
				}
			} else if (info instanceof OptIn) {
				// just repeat our last vote.
				getConnection().send(lastvotes);
			} else if (info instanceof OptInWithValue) {
				getConnection().send(lastvoteswithvalue);
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed to handle info", e);
		}
		updateRound(info);
	}

	@Override
	public Capabilities getCapabilities() {
		return new Capabilities(new HashSet<>(
				Arrays.asList("SAOP", "AMOP", "MOPAC", "MOPAC2", "Learn")),
				Collections.singleton(Profile.class));
	}

	@Override
	public String getDescription() {
		return "places random bids until it can accept an offer with utility >0.7. "
				+ "Parameters minPower and maxPower can be used to control voting behaviour.";
	}

	@Override
	public void terminate() {
		super.terminate();
		if (this.profileint != null) {
			this.profileint.close();
			this.profileint = null;
		}
	}

	//to get the set of bids on ParetoFrontier
	public Set<Bid> getOptimalPointsInParetoFrontier(List<UtilitySpace> listOfProfiles) {
		GenericParetoModified pareto = new GenericParetoModified(listOfProfiles);
		return pareto.getOptimalBids();
	}

	//to choose from our bidSpace according to the points on ParetoFrontier
	public Set<Bid> determineBidFromParetoFrontier(Set<Bid> paretoFront, UtilitySpace profile) {
		Iterator<Bid> itr = paretoFront.iterator();
		Set<Bid> goodBids = new HashSet<>();
		while (itr.hasNext()){
			if(selectBid(itr.next(), profile)){
				goodBids.add(itr.next());
			}
		}
		return goodBids;
	}
	public Bid bidToPlace(Set<Bid> paretoFront, UtilitySpace profile) throws IOException {
		Bid goodBid = null;
		if (paretoFront != null){
			Iterator<Bid> itr = paretoFront.iterator();
			Double utility = 0.0;
			if (itr.hasNext()) {
				goodBid = itr.next();
				utility = profile.getUtility(goodBid).doubleValue();
				while (itr.hasNext()) {
					Bid tempBid = itr.next();
					Double tempUtil = profile.getUtility(tempBid).doubleValue();
					if (tempUtil > utility) {
						goodBid = tempBid;
						utility = tempUtil;
					}
				}
			}
		}
		else{ //Pareto front not available because first time
			goodBid = placeRandomBid();
		}
		return goodBid;
	}

	private Bid placeRandomBid() throws IOException {
		AllPartialBidsList bidspace = new AllPartialBidsList(
				profileint.getProfile().getDomain());
		Bid bid = null;
		for (int attempt = 0; attempt < 20 && !isGood(bid); attempt++) {
			long i = random.nextInt(bidspace.size().intValue());
			bid = bidspace.get(BigInteger.valueOf(i));
		}
		return bid;
	}

	//to Check if utility bid is greater than reservation value
	public boolean selectBid(Bid bid, UtilitySpace profile){
		return profile.getUtility(bid).doubleValue() > this.reservationValue;
	}

	/******************* private support funcs ************************/

	/**
	 * Update {@link #progress}
	 *
	 * @param info the received info. Used to determine if this is the last info
	 *             of the round
	 */
	private void updateRound(Inform info) {
		if (protocol == null)
			return;
		switch (protocol) {
			case "SAOP":
			case "SHAOP":
				if (!(info instanceof YourTurn))
					return;
				break;
			case "MOPAC":
				if (!(info instanceof OptIn))
					return;
				break;
			case "MOPAC2":
				if (!(info instanceof OptInWithValue))
					return;
				break;
			default:
				return;
		}
		// if we get here, round must be increased.
		if (progress instanceof ProgressRounds) {
			progress = ((ProgressRounds) progress).advance();
		}

	}

	/**
	 * send our next offer
	 */
	private void makeOffer() throws IOException {
		Action action;
		/*if ((protocol.equals("SAOP") || protocol.equals("SHAOP"))
				&& isGood(lastReceivedBid)) {
			action = new Accept(me, lastReceivedBid);
		} else {
			// for demo. Obviously full bids have higher util in general
			AllPartialBidsList bidspace = new AllPartialBidsList(
					profileint.getProfile().getDomain());
			Bid bid = null;
			for (int attempt = 0; attempt < 20 && !isGood(bid); attempt++) {
				long i = random.nextInt(bidspace.size().intValue());
				bid = bidspace.get(BigInteger.valueOf(i));
			}*/

		//}
		Bid bid = null;
		if (firstBid) {
			bid = bidToPlace(null, null);
		} else {
			bid = bidToPlace(mostRecentOptimalPoints, (UtilitySpace) profileint.getProfile());
		}
		action = new Offer(me, bid);
		getConnection().send(action);

	}

	/**
	 * @param bid the bid to check
	 * @return true iff bid is good for us.
	 */
	private boolean isGood(Bid bid) {
		if (bid == null)
			return false;
		Profile profile;
		try {
			profile = profileint.getProfile();
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
		if (profile instanceof UtilitySpace)
			return ((UtilitySpace) profile).getUtility(bid).doubleValue() > this.reservationValue;
		if (profile instanceof PartialOrdering) {
			return ((PartialOrdering) profile).isPreferredOrEqual(bid,
					profile.getReservationBid());
		}
		return false;
	}

	/**
	 * @param voting the {@link Voting} object containing the options
	 * @return our next Votes.
	 */
	private Votes vote(Voting voting) throws IOException {
		if(firstBid){
			this.oppPowers = voting.getPowers();
			Integer sum = 0;
			for (PartyId id : oppPowers.keySet()) {
				sum = sum + oppPowers.get(id);
			}
			this.Pmax = sum;
			for (int i = 0; i < voting.getOffers().size(); i++) {
				FrequencyOpponentModel temp = new FrequencyOpponentModel().with(profileint.getProfile().getDomain(), null);
				PartyId partyId = voting.getOffers().get(i).getActor();
				opponentModelMap.put(partyId, temp);
			}
		} else {
			for (int i = 0; i < voting.getOffers().size(); i++) {
				FrequencyOpponentModel temp = new FrequencyOpponentModel().with(profileint.getProfile().getDomain(), null);
				PartyId partyId = voting.getOffers().get(i).getActor();
				FrequencyOpponentModel OM = opponentModelMap.get(partyId);
				OM = OM.with(voting.getOffers().get(i), progress);
				opponentModelMap.replace(partyId, OM);
			}
		}
		List<UtilitySpace> listOfProfiles = Arrays.asList();
		for (PartyId key : opponentModelMap.keySet()) {
			if (!key.equals(profileint)) {
				listOfProfiles.add(opponentModelMap.get(key));
			}
		}
		listOfProfiles.add((UtilitySpace) profileint.getProfile());
		Group60Party gp = new Group60Party();
		Set<Bid> optimalPoints = gp.getOptimalPointsInParetoFrontier(listOfProfiles);
		mostRecentOptimalPoints = optimalPoints;
		Object val = settings.getParameters().get("minPower");
		Integer minpower = (val instanceof Integer) ? (Integer) val : (Integer) (Pmax / 2);
		val = settings.getParameters().get("maxPower");
		Integer maxpower = (val instanceof Integer) ? (Integer) val
				: Pmax;

		Set<Vote> votes = voting.getOffers().stream().distinct()
				.filter(offer -> acceptOffer(offer.getBid(), optimalPoints))
				.map(offer -> new Vote(me, offer.getBid(), minpower, maxpower))
				.collect(Collectors.toSet());
		return new Votes(me, votes);
	}

	public boolean acceptOffer(Bid bid, Set<Bid> optimalPoints) {
		Profile profile;
		try {
			profile = profileint.getProfile();
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
		double util = 0;
		if (profile instanceof UtilitySpace)
			util = ((UtilitySpace) profile).getUtility(bid).doubleValue();
		if (bidWithThresholdOfOptimality(optimalPoints, bid, 0.8) && (util > this.reservationValue)) {
			return true;
		} else {
			return false;
		}

	}

	public Boolean bidWithThresholdOfOptimality(Set<Bid> optimalPoints, Bid bid, Double threshold) {
		return optimalPoints.stream().anyMatch(optimalBid -> {
			Map<String, Value> issueValues = bid.getIssueValues();
			return issueValues.keySet().stream().allMatch(issue -> {
				Value optimalIssueValue = issueValues.get(issue);
				Value bidIssueValue = optimalBid.getIssueValues().get(issue);
				if (optimalIssueValue == null || bidIssueValue == null) {
					return false;
				}
				if (optimalIssueValue instanceof NumberValue && bidIssueValue instanceof NumberValue) {
					double denom = Math.max(((NumberValue) optimalIssueValue).getValue().doubleValue(), ((NumberValue) bidIssueValue).getValue().doubleValue());
					double numer = Math.min(((NumberValue) optimalIssueValue).getValue().doubleValue(), ((NumberValue) bidIssueValue).getValue().doubleValue());
					if (numer / denom >= threshold) {
						return true;
					}
				} else if (optimalIssueValue instanceof DiscreteValue && bidIssueValue instanceof DiscreteValue) {
					return ((DiscreteValue) optimalIssueValue).getValue().equals(((DiscreteValue) bidIssueValue).getValue());
				}
				return false;
			});
		});
	}

	/**
	 * @param voting the {@link Voting} object containing the options
	 * @return our next Votes. Returns only votes on good bids and tries to
	 * distribute vote values evenly over all good bids.
	 */
	private VotesWithValue voteWithValue(Voting voting) throws IOException {
		Object val = settings.getParameters().get("minPower");
		Integer minpower = (val instanceof Integer) ? (Integer) val : 2;
		val = settings.getParameters().get("maxPower");
		Integer maxpower = (val instanceof Integer) ? (Integer) val
				: Integer.MAX_VALUE;

		List<Bid> goodbids = voting.getOffers().stream().distinct()
				.filter(offer -> isGood(offer.getBid()))
				.map(offer -> offer.getBid()).collect(Collectors.toList());

		if (goodbids.isEmpty()) {
			return new VotesWithValue(me, Collections.emptySet());
		}
		// extra difficulty now is to have the utility sum to exactly 100
		int mostvalues = 100 / goodbids.size();
		int value = 100 - mostvalues * (goodbids.size() - 1);
		Set<VoteWithValue> votes = new HashSet<>();
		for (Bid bid : goodbids) {
			votes.add(new VoteWithValue(me, bid, minpower, maxpower, value));
			value = mostvalues;
		}
		return new VotesWithValue(me, votes);
	}

}
