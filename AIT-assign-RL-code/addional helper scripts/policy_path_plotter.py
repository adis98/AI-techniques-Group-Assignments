import matplotlib.pyplot as plt
import pandas as pd
import numpy as np
import sys

if(len(sys.argv) > 1):
    rows = int(sys.argv[1])
    columns = int(sys.argv[2])
    csvPath = ""
    imagePath = "" #if you want to save the image
    if(len(sys.argv) < 4):
        print("Please mention the CSV filename as an argument with the extension (.csv) included")
        exit()
    else:
        csvPath = sys.argv[3]

    if(len(sys.argv) == 5):
        imagePath = sys.argv[4]
    data= pd.read_csv(csvPath,header=None)
    arr = data.values.tolist()
    row_index = 0
    x = 0
    y = rows - 1
    verts = [(x,y)]
    X = [x]
    Y = [y]
    visited = {} #dictionary that tracks all the states that have been visited

    #codes = [Path.MOVETO]
    column_index = 0
    state_index = row_index * columns + column_index
    visited[state_index] = True #mark the start state as "visited"
    done = False
    while done == False:
        row = arr[state_index][0].split(' ')
        maxIndex = 0
        print(state_index)
        for j in range(len(row)):
            if(float(row[j]) > float(row[maxIndex])):
                maxIndex = j
        if(maxIndex == 0):
            if(x != 0):
                x = x - 1
                column_index = column_index - 1
            #print("left")
        elif(maxIndex == 1):
            if(y != 0):
                y = y - 1
                row_index = row_index + 1
            #print("down")
        elif(maxIndex == 2):
            if(x != columns-1):
                column_index = column_index + 1
                x = x + 1
            #print("right")
        elif(maxIndex == 3):
            if(y != rows-1):
                row_index = row_index - 1
                y = y + 1
            #print("up")
        #print('x: ',x,' y: ',y)
        prev_state_index = state_index
        state_index = row_index * columns + column_index
        if(visited.get(state_index) is not None): #it has already been visited: warning break loop
            break
        else:
            visited[state_index] = True
        """
        if(state_index == prev_state_index):
            break"""
        #verts.append((x,y))
        #codes.append(Path.LINETO)
        X.append(x)
        Y.append(y)
        if(state_index == len(arr)-1):
            done = True
    plt.scatter(X,Y)
    plt.gca().set_xlim(left=-1)
    plt.gca().set_xlim(right=columns)
    plt.gca().set_ylim(bottom=-1)
    plt.gca().set_ylim(top=rows)
    plt.grid()
    plt.plot(X,Y)
    if(imagePath != ""):
        plt.savefig(imagePath)
    plt.show()
