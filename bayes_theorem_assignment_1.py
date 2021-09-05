#primary class for calculating probabilties using Bayes

class Bayes:
    
    def __init__(self, hypo=[], priors=[], obs=[], lkhood=[]): #Constructor for accepting inputs in a specified format
        self.hypo = hypo
        self.priors = priors
        self.obs = obs
        self.lkhood = lkhood

    def likelihood(self, observation, prior):
        return self.lkhood[self.hypo.index(prior)][self.obs.index(observation)] #lkhood input is in the form of an array within an array, so made use of upper and inner indexing.

    def norm_constant(self, observation): #chain rule
        norm_constant = sum([self.likelihood(observation, hypo) * self.priors[self.hypo.index(hypo)] for hypo in self.hypo])
        return round(norm_constant, 3)

    def single_posterior_update(self, observation): #calculates the posterior probabilities for each bowl, given a particular flavour has occurred
        posteriors = []
        norm_constant = self.norm_constant(observation)
        for bowl in self.hypo:
            temp = (self.likelihood(observation, bowl)*self.priors[self.hypo.index(bowl)])/norm_constant
            temp = round(temp, 3)
            posteriors.append(temp)
        return posteriors

    def compute_posterior(self, observations):
        posteriors = []
        for hypo in self.hypo:
            accumulator = self.priors[self.hypo.index(hypo)]
            for obv in observations:
                accumulator = accumulator * self.likelihood(obv, hypo)
            posteriors.append(accumulator)
        norm_const = sum(posteriors)
        return [round(posterior/norm_const, 3) for posterior in posteriors]

if __name__ == '__main__':
    hypos = ["Bowl1", "Bowl2"]
    priors = [0.5, 0.5]
    obs = ["chocolate", "vanilla"]
    # e.g. likelihood[0][1] corresponds to the likehood of Bowl1 and vanilla, or 35/50
    likelihood = [[15/50, 35/50], [30/50, 20/50]]
    b = Bayes(hypos, priors, obs, likelihood)
    l = b.likelihood("chocolate", "Bowl2")
    print("likelihood(chocolate, Bowl2) = %s " % l)

    norm_const = b.norm_constant("chocolate")
    print('norm_constant, p(chocolate) =',norm_const)

    posteriors = b.single_posterior_update("chocolate")
    print("posteriors are ", posteriors)

    posteriors = b.compute_posterior(["chocolate", "vanilla"])
    print("computed posteriors for chocolate, vanilla are ", posteriors)

    hypos = ["Beginner", "Intermediate", "Advanced", "Expert"]
    priors = [0.25, 0.25, 0.25, 0.25]
    obs = ["yellow", "red", "blue", "black", "white"]
    # e.g. likelihood[0][1] corresponds to the likehood of Bowl1 and vanilla, or 35/50
    likelihood = [[0.05 , 0.1, 0,4, 0.25, 0.2], [0.1, 0.2, 0.4, 0.2, 0.1], [0.2, 0.4, 0.25, 0.1, 0.05], [0.3, 0.5, 0.125, 0.05, 0.025]]
    b = Bayes(hypos, priors, obs, likelihood)

    l = b.likelihood("red", "Expert")
    print("likelihood(red, Expert) = %s " % l)

    norm_const = b.norm_constant("red")
    print('norm_constant, p(red) =', norm_const)

    posteriors = b.single_posterior_update("white")
    print("single posterior for white is ", posteriors)

    posteriors = b.compute_posterior(["yellow", "white", "blue", "red", "red", "blue"])
    print("posteriors for observed sequence are ", posteriors)



        
    
