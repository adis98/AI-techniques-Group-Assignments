package collabai.group60;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level;

import geniusweb.issuevalue.DiscreteValue;
import geniusweb.issuevalue.NumberValue;
import geniusweb.issuevalue.Value;
import geniusweb.opponentmodel.FrequencyOpponentModel;
import geniusweb.profile.utilityspace.UtilitySpace;
import geniusweb.profileconnection.ProfileInterface;
import geniusweb.progress.Progress;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import geniusweb.actions.Accept;
import geniusweb.actions.Action;
import geniusweb.actions.EndNegotiation;
import geniusweb.actions.Offer;
import geniusweb.actions.PartyId;
import geniusweb.actions.Votes;
import geniusweb.actions.VotesWithValue;
import geniusweb.bidspace.AllBidsList;
import geniusweb.connection.ConnectionEnd;
import geniusweb.inform.ActionDone;
import geniusweb.inform.Agreements;
import geniusweb.inform.Finished;
import geniusweb.inform.Inform;
import geniusweb.inform.OptIn;
import geniusweb.inform.Settings;
import geniusweb.inform.Voting;
import geniusweb.inform.YourTurn;
import geniusweb.issuevalue.Bid;
import geniusweb.party.Capabilities;
import geniusweb.profile.Profile;
import geniusweb.profile.utilityspace.LinearAdditive;
import geniusweb.progress.ProgressRounds;
import geniusweb.references.Parameters;
import geniusweb.references.ProfileRef;
import geniusweb.references.ProtocolRef;
import geniusweb.references.Reference;
import tudelft.utilities.listener.DefaultListenable;
import tudelft.utilities.logging.Reporter;

public class Group60PartyTest {

	private static final PartyId PARTY1 = new PartyId("party1");
	private static final String SAOP = "SAOP";
	private static final PartyId otherparty = new PartyId("other");
	private static final String PROFILE = "src/test/resources/testprofile.json";
	private static final String PROFILE1 = "src/test/resources/testprofile.json";
	private static final String PROFILE2 = "src/test/resources/testprofile2.json";
	private final static ObjectMapper jackson = new ObjectMapper();

	private Group60Party party;
	private final TestConnection connection = new TestConnection();
	private final ProtocolRef protocol = new ProtocolRef(SAOP);
	private final ProtocolRef mopacProtocol = new ProtocolRef("MOPAC");
	private final ProtocolRef mopac2Protocol = new ProtocolRef("MOPAC2");
	private final ProgressRounds progress = mock(ProgressRounds.class);
	private Settings settings, mopacSettings, mopac2Settings;
	private LinearAdditive profile;
	private LinearAdditive profile1;
	private LinearAdditive profile2;
	private final Parameters parameters = new Parameters();

	private final static String ISS1 = "Brand";
	private final static String ISS2 = "Harddisk";
	private static final DiscreteValue I1V1 = new DiscreteValue("Dell");
	private static final DiscreteValue I1V2 = new DiscreteValue("Macintosh");
	private static final DiscreteValue I2V1 = new DiscreteValue("128 GB");
	private static final DiscreteValue I2V2 = new DiscreteValue("512 GB");

	private static Bid bid1;
	private static Bid bid2;

	@Before
	public void before() throws JsonParseException, JsonMappingException,
			IOException, URISyntaxException {
		party = new Group60Party();
		settings = new Settings(PARTY1,
				new ProfileRef(new URI("file:" + PROFILE)), protocol, progress,
				parameters);
		mopacSettings = new Settings(PARTY1,
				new ProfileRef(new URI("file:" + PROFILE)), mopacProtocol,
				progress, parameters);
		mopac2Settings = new Settings(PARTY1,
				new ProfileRef(new URI("file:" + PROFILE)), mopac2Protocol,
				progress, parameters);

		String serialized = new String(Files.readAllBytes(Paths.get(PROFILE)),
				StandardCharsets.UTF_8);
		profile = (LinearAdditive) jackson.readValue(serialized, Profile.class);

		String serialized1 = new String(Files.readAllBytes(Paths.get(PROFILE1)),
				StandardCharsets.UTF_8);
		profile1 = (LinearAdditive) jackson.readValue(serialized1, Profile.class);

		String serialized2 = new String(Files.readAllBytes(Paths.get(PROFILE2)),
				StandardCharsets.UTF_8);
		profile2 = (LinearAdditive) jackson.readValue(serialized2, Profile.class);

		Map<String, Value> issuevalues = new HashMap<>();
		issuevalues.put(ISS1, I1V1);
		issuevalues.put(ISS2, I2V1);
		bid1 = new Bid(issuevalues);

		issuevalues.put(ISS1, I1V2);
		issuevalues.put(ISS2, I2V2);
		bid2 = new Bid(issuevalues);

	}

	@Test
	public void smokeTest() {
	}

	@Test
	public void callGetParetoFrontier() throws Exception {
		final String profile1 = "src/test/resources/party1.json";
		final String profile2 = "src/test/resources/party2.json";

		String serialized1 = new String(Files.readAllBytes(Paths.get(profile1)),
				StandardCharsets.UTF_8);
		String serialized2 = new String(Files.readAllBytes(Paths.get(profile2)),
				StandardCharsets.UTF_8);
		LinearAdditive linearAdd1 = (LinearAdditive) jackson.readValue(serialized1, Profile.class);
		LinearAdditive linearAdd2 = (LinearAdditive) jackson.readValue(serialized2, Profile.class);
		List<UtilitySpace> listOfProfiles = Arrays.asList(linearAdd1, linearAdd2);

		Group60Party gp = new Group60Party();
		Set<Bid> paretoFrontier = gp.getOptimalPointsInParetoFrontier(listOfProfiles);
		System.out.println(paretoFrontier);

		System.out.println(gp.determineBidFromParetoFrontier(paretoFrontier, linearAdd1));
	}

	@Test
	public void acceptNumericBidsWithThreshold() throws Exception {

		List<UtilitySpace> listOfProfiles = Arrays.asList(profile1, profile2);

		Group60Party gp = new Group60Party();
		ProfileInterface profileInterfaceMock = mock(ProfileInterface.class);
		when(profileInterfaceMock.getProfile()).thenReturn(profile);
		gp.profileint = profileInterfaceMock;
		gp.reservationValue = 0;
		Map<String, Value> issuevalues = new HashMap<>();
		issuevalues.put("issue2", new NumberValue(new BigDecimal(32)));
		issuevalues.put("issue1", new DiscreteValue("issue1value1"));
		Bid testBid1 = new Bid(issuevalues);

		issuevalues.put("issue2", new NumberValue(new BigDecimal(11)));
		issuevalues.put("issue1", new DiscreteValue("issue1value2"));
		Bid testBid2 = new Bid(issuevalues);

		issuevalues.put("issue2", new NumberValue(new BigDecimal(16)));
		issuevalues.put("issue1", new DiscreteValue("issue1value1"));
		Bid testBid3 = new Bid(issuevalues);

		issuevalues.put("issue2", new NumberValue(new BigDecimal(32)));
		issuevalues.put("issue1", new DiscreteValue("issue1value2"));
		Bid testBid4 = new Bid(issuevalues);

		Set<Bid> paretoFrontier = gp.getOptimalPointsInParetoFrontier(listOfProfiles);
		System.out.println(paretoFrontier);
		System.out.println(gp.bidWithThresholdOfOptimality(paretoFrontier, testBid1, 0.8, profile1));
		System.out.println(gp.bidWithThresholdOfOptimality(paretoFrontier, testBid2, 0.8, profile1));
		System.out.println(gp.bidWithThresholdOfOptimality(paretoFrontier, testBid3, 0.8, profile1));
		System.out.println(gp.bidWithThresholdOfOptimality(paretoFrontier, testBid4, 0.8, profile1));

	}

	@Test
	public void getParetoFrontierWithFrequencyOpponentModel() throws Exception {
		final String profile1 = "src/test/resources/testprofile.json";

		String serialized1 = new String(Files.readAllBytes(Paths.get(profile1)),
				StandardCharsets.UTF_8);
		LinearAdditive linearAdd1 = (LinearAdditive) jackson.readValue(serialized1, Profile.class);

		PartyId partyId = new PartyId("Opponent");
		Offer offer = new Offer(partyId, bid1);
		Offer offer1 = new Offer(partyId, bid1);
		Offer offer2 = new Offer(partyId, bid2);


		FrequencyOpponentModel opp1 = new FrequencyOpponentModel()
				.with(linearAdd1.getDomain(),null) //Always initialize with the domain and reservation bid
				.with(offer,progress)
				.with(offer1,progress)
				.with(offer2,progress);//Whenever we get an offer we apply this to the corresponent opponent.

		FrequencyOpponentModel opp2 = new FrequencyOpponentModel()
				.with(linearAdd1.getDomain(),null) //Always initialize with the domain and reservation bid
				.with(offer2,progress)
				.with(offer1,progress)
				.with(offer2,progress);//Whenever we get an offer we apply this to the corresponent opponent.

		Group60Party gp = new Group60Party();
		List<UtilitySpace> listOfProfiles = Arrays.asList(opp1, opp2);
		Set<Bid> paretoFrontier = gp.getOptimalPointsInParetoFrontier(listOfProfiles);
		System.out.println(paretoFrontier);
		System.out.println(gp.determineBidFromParetoFrontier(paretoFrontier, linearAdd1));
	}

	@Test
	public void getFrequencyOpponentModelsUtilities() throws IOException {
		final String profile1 = "src/test/resources/party1.json";

		String serialized1 = new String(Files.readAllBytes(Paths.get(profile1)),
				StandardCharsets.UTF_8);
		LinearAdditive linearAdd1 = (LinearAdditive) jackson.readValue(serialized1, Profile.class);

		PartyId partyId = new PartyId("Opponent");
		Offer offer = new Offer(partyId, bid1);
		Offer offer1 = new Offer(partyId, bid1);
		Offer offer2 = new Offer(partyId, bid2);


		FrequencyOpponentModel opp1 = new FrequencyOpponentModel()
				.with(linearAdd1.getDomain(),null) //Always initialize with the domain and reservation bid
				.with(offer,progress) //Whenever we get an offer we apply this to the corresponent opponent.
				.with(offer1,progress)
				.with(offer2,progress); //In this case, the opponent has made the same bid twice and another bid 1 more time

		System.out.println(opp1.getUtility(bid1));
		System.out.println(opp1.getUtility(bid2)); //This value is half of the first one because we only used it once
		//They don't add up to one because in both cases we only use 2 issues.

	}

	@Test
	public void getDescriptionTest() {
		assertNotNull(party.getDescription());
	}

	@Test
	public void getCapabilitiesTest() {
		Capabilities capabilities = party.getCapabilities();
		assertFalse("party does not define protocols",
				capabilities.getBehaviours().isEmpty());
	}

	@Test
	public void testInformConnection() {
		party.connect(connection);
		// agent should not start acting just after an inform
		assertEquals(0, connection.getActions().size());
	}

	@Test
	public void testInformSettings() {
		party.connect(connection);
		connection.notifyListeners(settings);
		assertEquals(0, connection.getActions().size());
	}

	@Test
	public void testInformAndConnection() {
		party.connect(connection);
		party.notifyChange(settings);
		assertEquals(0, connection.getActions().size());
	}

	@Test
	public void testOtherWalksAway() {
		party.connect(connection);
		party.notifyChange(settings);

		party.notifyChange(new ActionDone(new EndNegotiation(otherparty)));

		// party should not act at this point
		assertEquals(0, connection.getActions().size());
	}

	@Test
	public void testAgentHasFirstTurn() {
		party.connect(connection);
		party.notifyChange(settings);

		party.notifyChange(new YourTurn());
		assertEquals(1, connection.getActions().size());
		assertTrue(connection.getActions().get(0) instanceof Offer);
	}

	@Test
	public void testAgentAccepts() {
		party.connect(connection);
		party.notifyChange(settings);

		Bid bid = findGoodBid();
		party.notifyChange(new ActionDone(new Offer(otherparty, bid)));
		party.notifyChange(new YourTurn());
		assertEquals(1, connection.getActions().size());
		assertTrue(connection.getActions().get(0) instanceof Accept);

	}

	@Test
	public void testAgentLogsFinal() {
		// this log output is optional, this is to show how to check log
		Reporter reporter = mock(Reporter.class);
		party = new Group60Party(reporter);
		party.connect(connection);
		party.notifyChange(settings);
		Agreements agreements = mock(Agreements.class);
		when(agreements.toString()).thenReturn("agree");
		party.notifyChange(new Finished(agreements));

		verify(reporter).log(eq(Level.INFO),
				eq("Final ourcome:Finished[agree]"));
	}

	@Test
	public void testAgentsUpdatesSAOPProgress() {
		party.connect(connection);
		party.notifyChange(settings);

		party.notifyChange(new YourTurn());
		verify(progress).advance();
	}

	@Test
	public void testAgentsUpdatesMOPACProgress() {
		party.connect(connection);
		party.notifyChange(mopacSettings);
		// in mopac, progress happens only after optin phase
		party.notifyChange(new YourTurn());
		verify(progress, times(0)).advance();
		party.notifyChange(
				new Voting(Collections.emptyList(), Collections.emptyMap()));
		verify(progress, times(0)).advance();
		party.notifyChange(new OptIn(Collections.emptyList()));
		verify(progress, times(1)).advance();
	}

	@Test
	public void testGetCapabilities() {
		assertTrue(party.getCapabilities().getBehaviours().contains(SAOP));
	}

	private Bid findGoodBid() {
		for (Bid bid : new AllBidsList(profile.getDomain())) {
			if (profile.getUtility(bid)
					.compareTo(BigDecimal.valueOf(0.7)) > 0) {
				return bid;
			}
		}
		throw new IllegalStateException(
				"Test can not be done: there is no good bid with utility>0.7");
	}

	@Test
	public void testVoting() throws URISyntaxException {
		party.connect(connection);
		party.notifyChange(mopacSettings);

		Bid bid = findGoodBid();
		Offer offer = new Offer(PARTY1, bid);
		party.notifyChange(new Voting(Arrays.asList(offer),
				Collections.singletonMap(PARTY1, 1)));
		assertEquals(1, connection.getActions().size());
		Action action = connection.getActions().get(0);
		assertTrue(action instanceof Votes);
		assertEquals(1, ((Votes) action).getVotes().size());
		assertEquals(bid,
				((Votes) action).getVotes().iterator().next().getBid());
	}

	@Test
	public void testVotingWithValue() throws URISyntaxException {
		party.connect(connection);
		party.notifyChange(mopac2Settings);

		Bid bid = findGoodBid();
		Offer offer = new Offer(PARTY1, bid);
		party.notifyChange(new Voting(Arrays.asList(offer),
				Collections.singletonMap(PARTY1, 1)));
		assertEquals(1, connection.getActions().size());
		Action action = connection.getActions().get(0);
		assertTrue(action instanceof VotesWithValue);
		assertEquals(1, ((VotesWithValue) action).getVotes().size());
		assertEquals(bid, ((VotesWithValue) action).getVotes().iterator().next()
				.getBid());
	}
}

/**
 * A "real" connection object, because the party is going to subscribe etc, and
 * without a real connection we would have to do a lot of mocks that would make
 * the test very hard to read.
 *
 */
class TestConnection extends DefaultListenable<Inform>
		implements ConnectionEnd<Inform, Action> {
	private List<Action> actions = new LinkedList<>();

	@Override
	public void send(Action action) throws IOException {
		actions.add(action);
	}

	@Override
	public Reference getReference() {
		return null;
	}

	@Override
	public URI getRemoteURI() {
		return null;
	}

	@Override
	public void close() {

	}

	@Override
	public Error getError() {
		return null;
	}

	public List<Action> getActions() {
		return actions;
	}

}
