import pystablemotifs as sm
import pyboolnet
import pystablemotifs.export as ex
import pystablemotifs.format as format
from pyboolnet import attractors

# store the attractor dict into a csv file
def dict2csv(input_BN,fixed):
    import csv
    with open(input_BN+'_attractor.csv', 'w', newline='') as myfile:
     wr = csv.writer(myfile, quoting=csv.QUOTE_ALL)
     header = ['Node','State']
     wr.writerow(header)
     for node in fixed:
         list = [node.replace("_fc",''), fixed.get(node)]
         wr.writerow(list)

#import the model and print boolean rules
input_BN = "TopStrain_GoldStandardGRNbased"
print("Importing the Boolean model")
primes = sm.format.import_primes('/Users/yzongy/BooleanNetworks/'+input_BN+'.txt')

#fixed nodes and find the attractor
print("finding attractors")
fixed = {}
inhibited = []
for node in primes:
    list = primes.get(node)[0]
    if list==[]:
        fixed[node] = 1 # not being regulated by other genes, and also in the genome
    else:
        for key in list[0]:
            if list[0].get(key) == 1:
                fixed[node] = 0 # inhibited by an fixed existing node
                inhibited.append(node)
            if list[0].get(key) == 0:
                if node not in inhibited:
                    fixed[node] = 1 # activated by an fixed existing node
attractor = fixed

#export to csv
dict2csv(input_BN,fixed)
