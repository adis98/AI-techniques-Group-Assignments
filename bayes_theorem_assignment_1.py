#primary class for calculating probabilties using Bayes

def write_answer(txt, answer):
    txt.write(str(answer))
    txt.write("\n")


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
    txt = open("group_60.txt", "w")
    # just use write_answer(txt, [the answer])
    cookie_hypos = ["Bowl1", "Bowl2"]
    cookie_priors = [0.5, 0.5]
    cookie_obs = ["chocolate", "vanilla"]
    cookie_likelihood = [[15 / 50, 35 / 50], [30 / 50, 20 / 50]]
    cookie_b = Bayes(cookie_hypos, cookie_priors, cookie_obs, cookie_likelihood)

    q1 = cookie_b.single_posterior_update('vanilla')[0]
    write_answer(txt, q1)

    q2 = cookie_b.compute_posterior(['vanilla', 'chocolate'])[1]
    write_answer(txt, q2)


    archer_hypos = ["Beginner", "Intermediate", "Advanced", "Expert"]
    archer_priors = [0.25, 0.25, 0.25, 0.25]
    archer_obs = ["yellow", "red", "blue", "black", "white"]
    # e.g. likelihood[0][1] corresponds to the likehood of Bowl1 and vanilla, or 35/50
    archer_likelihood = [[0.05 , 0.1, 0.4, 0.25, 0.2], [0.1, 0.2, 0.4, 0.2, 0.1], [0.2, 0.4, 0.25, 0.1, 0.05], [0.3, 0.5, 0.125, 0.05, 0.025]]
    archer_b = Bayes(archer_hypos, archer_priors, archer_obs, archer_likelihood)

    q3 = archer_b.compute_posterior(['yellow', 'white', 'blue', 'red', 'red', 'blue'])[1]
    write_answer(txt, q3)

    temp = archer_b.compute_posterior(['yellow', 'white', 'blue', 'red', 'red', 'blue'])
    q4 = archer_hypos[temp.index(max(temp))]
    write_answer(txt, q4)

    txt.close()



        
    
