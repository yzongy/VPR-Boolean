import pystablemotifs as sm
import pyboolnet
import pystablemotifs.export as ex
import pystablemotifs.format as format
import networkx as nx
from pyboolnet import interaction_graphs as IGs
from pyboolnet.interaction_graphs import primes2igraph, igraph2image, igraph2dot
import matplotlib.pyplot as plt

# convert the unpivot attractor csv file to dictionary as the target_state
def csv2dict(AttractorsCSVfile):
    import csv
    with open(AttractorsCSVfile+'.csv', mode='r') as infile:
        reader = csv.reader(infile,delimiter='\t')
        next(reader)
        for row in reader:
            print(row)
            mydict = {row[0]:row[1]}
        print(mydict)
    return mydict
# store the controlStrategies list into a csv file
def list2csv(input_BN,controlStrategies):
    import csv
    with open(input_BN+'_controlStrategies.csv', 'w', newline='') as myfile:
     wr = csv.writer(myfile, quoting=csv.QUOTE_ALL)
     wr.writerow(controlStrategies)


#import the model and print boolean rules
input_BN = "processed_output_boolNet_wildtype_GRNBased"
print("Importing the Boolean model")
primes = sm.format.import_primes('/Users/yzongy/BooleanNetworks/'+input_BN+'.txt')
import json
with open(input_BN+'primes.json', 'w') as fp:
    json.dump(primes, fp)

# # print("converting to image")
# # igraph = primes2igraph(primes)
# # igraph2image(igraph, input_BN+".pdf", layout_engine="fdp")

print("finding attractors")
max_simulate_size=20
ar = sm.AttractorRepertoire.from_primes(primes, max_simulate_size=max_simulate_size,max_stable_motifs=20000,MPBN_update=True)

print("printing all attractors")
df=ex.attractor_dataframe(ar)
df.to_csv(input_BN+'.csv')
print("attractors have been exported to CSV")


# print("printing the state transition graph")
# for a in ar.attractors:
#     if a.n_unfixed == 0: continue # skip steady states
#     # print(a.attractor_dict)
#     nx.draw(a.stg,with_labels=True)
#     plt.savefig(input_BN+"_Graph.png", format="PNG")


print("finding target control strategies")
target_state={'YDL127W_fc': 0, 'YDR245W_fc': 0, 'YNL048W_fc': 0}
# AttractorsCSVfile = "unpivot_processed_TopStrain"
# target_state = csv2dict(AttractorsCSVfile)
print(ar.succession_diagram.reprogram_to_trap_spaces(logically_fixed=target_state,
                                               target_method='history',
                                               driver_method='internal'))
# print("storing control strategies into csv")
# list2csv(input_BN,controlStrategies)
