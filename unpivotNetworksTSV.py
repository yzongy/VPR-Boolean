import pandas as pd

file = 'GoldstandardGRN'
df=pd.read_csv(file+'.tsv', index_col=0,sep='\t')
print(df)
unpivot=df.stack().reset_index().rename(columns={'level_0':'Target','level_1':'Source', 0:'Weight'})
print(unpivot)

# output = pd.DataFrame([unpivot.Weight != 0])
# print(output)
unpivot = unpivot[unpivot.Weight != 0]
unpivot.to_csv('unpivot_'+file+'.tsv', sep = '\t')
