import copy
import random
from simple_grid import REWARD
import numpy as np

NUM_EPISODES = 10000
MAX_EPISODE_LENGTH = 500


DEFAULT_DISCOUNT = 0.9
EPSILON = 0.05
BETA = 800
LEARNINGRATE = 0.1




class QLearner():
    """
    Q-learning agent
    """
    def __init__(self, num_states, num_actions, discount=DEFAULT_DISCOUNT, learning_rate=LEARNINGRATE):
        self.name = "agent1"
        self.num_states = num_states
        self.num_actions = num_actions
        self.discount = discount
        self.learning_rate = learning_rate
        self.Q = np.zeros((num_states, num_actions))



    def reset_episode(self):
        """
        Here you can update some of the statistics that could be helpful to maintain
        """
        pass




    def process_experience(self, state, action, next_state, reward, done):
        """
        Update the Q-value based on the state, action, next state and reward.
        """
        if done:
            self.Q[state,action] = (1-LEARNINGRATE)*self.Q[state,action]+LEARNINGRATE*reward
        else:
            self.Q[state,action] = (1- LEARNINGRATE)*self.Q[state,action] + LEARNINGRATE*(reward + DEFAULT_DISCOUNT*np.max(self.Q[next_state,:]))



    def select_action(self, state):
        """
        Returns an action, selected based on the current state
        """

        #Epsilon greedy
        # if random.uniform(0,1) < EPSILON:
        #     return random.randint(0,self.num_actions-1)

        #softmax
        if random.uniform(0,1) < EPSILON:
            allQExp_valuesSum = np.sum(np.exp(np.true_divide((self.Q[state,:]), BETA)))
            allWeights = []
            for a in range(0, self.num_actions-1):
                q_value = self.Q[state,a]
                eachWeight = np.true_divide(np.exp(np.true_divide(q_value, BETA)), allQExp_valuesSum)
                allWeights.append(eachWeight)
            allWeightsCDF = np.cumsum(allWeights)
            randIntForSoftMax = random.uniform(0,1)
            for i in range(0,self.num_actions-1):
                if((i == 0) and (randIntForSoftMax <= allWeightsCDF[i] and randIntForSoftMax > 0)):
                    return i
                if(randIntForSoftMax <= allWeightsCDF[i] and randIntForSoftMax > allWeightsCDF[i-1]):
                    return i

        q_max = np.max(self.Q[state,:])
        if q_max == 0: #In this case, none of the actions have been explored so we chose one at random
            return random.randint(0,self.num_actions-1)

        a_max = 0
        for a in range(0,self.num_actions):
            q_value = self.Q[state,a]
            if q_value == q_max:
                a_max = a
                break

        return a_max

    def value_iteration(self, env, error = 1e-10):
            Q_new, Q_old = np.zeros((self.num_states, self.num_actions)), np.zeros((self.num_states, self.num_actions))
            for t in range(NUM_EPISODES):
                Q_old = copy.deepcopy(Q_new)
                delta = 0
                for s in range(0,self.num_states-1):
                    for a in range(0,self.num_actions):
                        value = 0
                        for i in env.P[s][a]:
                            value += i[0]*(i[2]+ DEFAULT_DISCOUNT*np.max(Q_old[i[1],:]))
                        Q_new[s,a] = value
                        delta = max(delta, abs(Q_old[s,a]-Q_new[s,a]))
                if delta < error:
                    break
            return Q_new

    def report(self):
        """
        Function to print useful information, printed during the main loop
        """

        #print(self.Q)
