import pystablemotifs as sm
import pyboolnet
import pystablemotifs.export as ex
import pystablemotifs.format as format
import networkx as nx
from pyboolnet import interaction_graphs as IGs
from pyboolnet.interaction_graphs import primes2igraph, igraph2image, igraph2dot
import matplotlib.pyplot as plt


#import the model and print boolean rules
input_BN = "simpleModel"
print("Importing the Boolean model")
primes = sm.format.import_primes('/Users/yzongy/BooleanNetworks/'+input_BN+'.txt')
# import json
# with open(input_BN+'primes.json', 'w') as fp:
#     json.dump(primes, fp)

print("converting to image")
igraph = primes2igraph(primes)
igraph2image(igraph, input_BN+".pdf", layout_engine="dot")

print("finding attractors")
max_simulate_size=9
ar = sm.AttractorRepertoire.from_primes(primes, max_simulate_size=max_simulate_size)

print("printing all attractors")
df=ex.attractor_dataframe(ar)
df.to_csv(input_BN+'_attractors.csv')
print("attractors have been exported to CSV")


print("printing the state transition graph")
for a in ar.attractors:
    if a.n_unfixed == 0: continue # skip steady states
    # print(a.attractor_dict)
    nx.draw(a.stg,with_labels=True)
    plt.savefig(input_BN+"_Graph.png", format="PNG")

print("finding target control strategies")
target_state={'xA': 1, 'xB': 1}
print(ar.succession_diagram.reprogram_to_trap_spaces(logically_fixed=target_state,
                                               target_method='history',
                                               driver_method='internal'))
