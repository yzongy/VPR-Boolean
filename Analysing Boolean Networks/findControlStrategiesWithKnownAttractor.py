import pyboolnet as pbn
import pystablemotifs as psm

#find the attractor of a targte strain(e.g. high fitness strain)
#import the model
input_BN = "TopStrain_GoldStandardGRNbased"
print("Importing the Boolean model")
primes = psm.format.import_primes('/Users/yzongy/BooleanNetworks/'+input_BN+'.txt')
#an easy way to find attractor for GRN based model
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
target_state = fixed

# import the strain to be evolved
input_BN_st = "wildtype_GoldStandarGRNbased"
print("Importing the Boolean model of the starting strain")
primes_st = psm.format.import_primes('/Users/yzongy/BooleanNetworks/'+input_BN_st+'.txt')
# find control strategies
print("Easier way to caculate control strategies:")
stable_motifs = pbn.trap_spaces.compute_trap_spaces(primes_st, "max")
driverList = []
for motif in stable_motifs:
    if psm.drivers.fixed_implies_implicant(target_state, motif):
      motif_drivers = psm.drivers.internal_drivers(motif,primes_st)
      driverList.append(motif_drivers)
print(driverList)

#store the control strategy into csv
import csv
with open(input_BN_st+'_to_'+input_BN+'_controlStrategies.csv', 'w', newline='') as myfile:
    wr = csv.writer(myfile, quoting=csv.QUOTE_ALL)
    header = ['Node','controlStrategies']
    wr.writerow(header)
    tempList = []
    for item in driverList:
        for dict in item:
            for node in dict:
                if node in tempList:
                    continue
                else:
                    line = [node.replace("_fc",""),dict.get(node)]
                    wr.writerow(line)
                    tempList.append(node)
