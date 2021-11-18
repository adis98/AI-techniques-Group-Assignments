import random

import numpy as np

NUM_EPISODES = 1000
MAX_EPISODE_LENGTH = 500


DEFAULT_DISCOUNT = 0.9
EPSILON = 0.05
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

        if random.uniform(0,1) < EPSILON:
            return random.randint(0,self.num_actions-1)

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



    def report(self):
        """
        Function to print useful information, printed during the main loop
        """
        pass







        
