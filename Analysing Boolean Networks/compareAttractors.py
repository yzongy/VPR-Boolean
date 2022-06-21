import pystablemotifs as sm
import pyboolnet
import pystablemotifs.export as ex
import pystablemotifs.format as format
import networkx as nx
from pyboolnet import interaction_graphs as IGs
from pyboolnet import state_transition_graphs
from pyboolnet import attractors
from pyboolnet import basins_of_attraction
from pyboolnet.interaction_graphs import primes2igraph, igraph2image, igraph2dot
import matplotlib.pyplot as plt

#import the model and print boolean rules
input_BN = "BottomStrain_GoldStandardGRNbased"
print("Importing the Boolean model")
primes = sm.format.import_primes('/Users/yzongy/BooleanNetworks/'+input_BN+'.txt')

print("finding attractors")
#fixed nodes
fixed = {}
for node in primes:
    list = primes.get(node)[0]
    if list==[]:
        fixed[node] = 1
        print("set to 1: "+ node)
    else:
        for key in list[0]:
            if list[0].get(key) == 1:
                fixed[node] = 0
                print("set to 0: "+ node)
            if list[0].get(key) == 0:
                fixed[node] = 1
                print("set to 1: "+ node)
attractor = fixed

#import the model and print boolean rules
input_BN_2 = "BottomStrain_GoldStandardGRNbased"
print("Importing the Boolean model")
primes_2 = sm.format.import_primes('/Users/yzongy/BooleanNetworks/'+input_BN_2+'.txt')

print("finding attractors")
#fixed nodes
fixed_2 = {}
for node in primes_2:
    list = primes_2.get(node)[0]
    if list==[]:
        fixed[node] = 1
        print("set to 1: "+ node)
    else:
        for key in list[0]:
            if list[0].get(key) == 1:
                fixed_2[node] = 0
                print("set to 0: "+ node)
            if list[0].get(key) == 0:
                fixed_2[node] = 1
                print("set to 1: "+ node)
attractor_2 = fixed_2

#compare
difference = []
for item in attractor:
    if attractor.get(item) != attractor_2.get(item):
        difference.append(item)
print(difference)
