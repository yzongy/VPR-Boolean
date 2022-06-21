import pystablemotifs as sm
import pyboolnet
import pystablemotifs.export as ex
import networkx as nx
from pyboolnet import interaction_graphs as IGs
from pyboolnet.interaction_graphs import primes2igraph, igraph2image, igraph2dot


#import the model and print boolean rules
print("Importing the Boolean model")
# sm.format.pretty_print_prime_rules(
#     # sm.format.import_primes('/Users/yzongy/BooleanNetworks/2Models/2ModelsBoolExp.txt'))
#     sm.format.import_primes('/Users/yzongy/vpr2/virtualparts.boolean/output_boolNet.txt'))


#find attractors
# print("finding attractors")
primes = sm.format.import_primes('/Users/yzongy/vpr2/virtualparts.boolean/output_boolNet.txt')
print("Converting to igraph")
igraph = primes2igraph(primes)
print("Converting to dot")
igraph2dot(igraph, "output_boolNet.dot")
# print(igraph)
# igraph2image(igraph, "test_simpleModel.pdf")

# max_simulate_size=10
# ar = sm.AttractorRepertoire.from_primes(primes, max_simulate_size=max_simulate_size)
#
# print("printing all attractors")
# df=ex.attractor_dataframe(ar)
# print(df)
