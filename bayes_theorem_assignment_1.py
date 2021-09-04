#primary class for calculating probabilties using Bayes

class Bayes:
    
    def __init__(self, hypo=[], priors=[], obs=[], lkhood=[]): #Constructor for accepting inputs in a specified format
        self.hypo = hypo
        self.priors = priors
        self.obs = obs
        self.lkhood = lkhood

    def likelihood(self,observation, prior):
        upperindex = self.hypo.index(prior)
        innerindex = self.obs.index(observation)
        return self.lkhood[upperindex][innerindex] #lkhood input is in the form of an array within an array, so made use of upper and inner indexing. 

    def norm_constant(self,observation): #chain rule
        accumulator = 0
        for bowl in self.hypo:
            accumulator = accumulator + self.likelihood(observation, bowl) * self.priors[self.hypo.index(bowl)]
        accumulator = round(accumulator,3)
        return accumulator

    def single_posterior_update(self,observation): #calculates the posterior probabilities for each bowl, given a particular flavour has occurred
        posteriors = []
        norm_const = self.norm_constant(observation)
        for bowl in self.hypo:
            temp = (self.likelihood(observation,bowl)*self.priors[self.hypo.index(bowl)])/norm_const
            temp = round(temp,3)
            posteriors.append(temp)
        return posteriors

    '''TODO: The last function in Q1, Testing, Verification, optimization'''


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
    print("posteriors are ",posteriors)

        
    
