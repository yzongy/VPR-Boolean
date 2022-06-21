import pystablemotifs as sm
import pyboolnet
import pystablemotifs.format as format
import pyboolnet.model_checking

print(pyboolnet.version.read_version())

#import the model and print boolean rules
input_BN = "BottomStrain_GoldStandardGRNbased"
print("Importing the Boolean model")
primes = sm.format.import_primes('/Users/yzongy/BooleanNetworks/'+input_BN+'.txt')

print("model checking")
pyboolnet.model_checking.primes2smv(primes, "synchronous",  "INIT TRUE", "LTLSPEC F(YLR131C_fc)", "test.smv")
print(pyboolnet.model_checking.model_checking_smv_file("test.smv"))
