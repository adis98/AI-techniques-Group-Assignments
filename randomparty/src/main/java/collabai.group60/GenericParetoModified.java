package collabai.group60;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import geniusweb.bidspace.AllBidsList;
import geniusweb.bidspace.pareto.ParetoFrontier;
import geniusweb.issuevalue.Bid;
import geniusweb.issuevalue.Domain;
import geniusweb.issuevalue.Value;
import geniusweb.profile.PartialOrdering;
import geniusweb.profile.Profile;
import geniusweb.profile.utilityspace.UtilitySpace;

/**
 * A set of all pareto points associated with a set of profiles. Generally
 * applicable but slow.
 */
public class GenericParetoModified implements ParetoFrontier {

    private final List<? extends PartialOrdering> profiles;
    private Set<Bid> paretobids = null;

    /**
     * Constructs pareto from a general {@link PartialOrdering}. This is a
     * brute-force algorithm that will inspect (but not store) ALL bids in the
     * space, and therefore may be inapproproate depending on the size of your
     * domain. Complexity O( |bidspace|^2 |profiles| )
     *
     * @param profiles a set of at least 2 {@link PartialOrdering}s. All must be
     *                 defined on the same domain.
     */
    public GenericParetoModified(List<? extends PartialOrdering> profiles) {
        if (profiles.size() < 2) {
            throw new IllegalArgumentException(
                    "at least 2 profiles are needed");
        }
        if (profiles.stream().anyMatch(profile -> profile == null)) {
            throw new IllegalArgumentException("Profiles must not be null");
        }
        Domain domain = profiles.get(0).getDomain();
        for (PartialOrdering profile : profiles) {
            if (!domain.equals(profile.getDomain())) {
                throw new IllegalArgumentException(
                        "All profiles must be on same domain ("
                                + domain.getName() + ") but found " + profile);
            }
        }
        this.profiles = profiles;
    }

    @Override
    public List<? extends Profile> getProfiles() {
        return profiles;
    }

    @Override
    public synchronized Set<Bid> getPoints() {
        if (paretobids == null)
            computePareto();
        return paretobids;
    }

    public synchronized Set<Bid> getOptimalBids() {
        if (paretobids == null)
            computePareto();
        List<Bid> paretoBidsAsList = new ArrayList<>(paretobids);
        List<List<Double>> doubleStream = paretoBidsAsList.stream()
                .map(this::issueSum)
                .collect(Collectors.toList());
        List<Double> optimalDifferences = doubleStream.stream()
                .map(this::optimalDifference)
                .collect(Collectors.toList());
        double min = optimalDifferences.stream().mapToDouble(Double::doubleValue).min().getAsDouble();//-6.0;
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < optimalDifferences.size(); i++) {
            if (optimalDifferences.get(i) == min) {
                indices.add(i);
            }
        }
        Set<Bid> resultBids = new HashSet<>();
        indices.forEach(index -> resultBids.add(paretoBidsAsList.get(index)));
        return resultBids;
    }

    @Override
    public String toString() {
        return "Pareto " + getPoints();
    }

    /**
     * assigns {@link #paretobids}.
     */
    private void computePareto() {
        paretobids = new HashSet<Bid>();

        for (Bid newbid : new AllBidsList(profiles.get(0).getDomain())) {
            /*
             * invariant: paretobids contains bids not dominated by other bids
             * in paretobids. That means we need (1) check if new bid is
             * dominated (2) if existing bids are dominated if we add a new bid
             */
            boolean newBidIsDominated = paretobids.stream()
                    .anyMatch(paretobid -> isDominatedBy(newbid, paretobid));

            // if new bid is not dominated, we add it and we re-check existing
            // if they are now dominated
            if (!newBidIsDominated) {
                paretobids = paretobids.stream()
                        .filter(paretobid -> !isDominatedBy(paretobid, newbid))
                        .collect(Collectors.toSet());
                paretobids.add(newbid);
            }
        }
    }

    /**
     * @param bid1        the bid to check
     * @param dominantbid the bid that is supposed to dominate bid1
     * @return true iff bid1 is dominated by dominant bid. "Dominated by" means
     * that the bid is preferred or equal in all profiles.
     */
    private boolean isDominatedBy(Bid bid1, Bid dominantbid) {
        return profiles.stream().allMatch(
                profile -> profile.isPreferredOrEqual(dominantbid, bid1));
    }

    private List<Double> issueSum(Bid bid) {
        return profiles.stream().map(
                profile -> {
                    UtilitySpace profileForced = (UtilitySpace) profile;
                    return ((UtilitySpace) profile).getUtility(bid).doubleValue();
                }).collect(Collectors.toList());
    }

    private Double optimalDifference(List<Double> profileIssueValue) {
        List<Double> pairwiseDifferences = new ArrayList<>();
        for (int i = 0; i < profileIssueValue.size() - 1; i++) {
            for (int j = i + 1; j < profileIssueValue.size(); j++) {
                pairwiseDifferences.add(Math.abs(profileIssueValue.get(i) - profileIssueValue.get(j)));
            }
        }
        return sum(pairwiseDifferences);
    }

    public double sum(List<Double> list) {
        double sum = 0;

        for (double i : list)
            sum = sum + i;
        return sum;
    }
}
