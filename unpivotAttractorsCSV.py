import pandas as pd

attractorCSV = 'processed_BottomStrain_GRNbased_Attractors'
df=pd.read_csv(attractorCSV+'.csv', index_col=0,sep=',')
print(df)
print(df.T)
output = df.T
output.to_csv('unpivot_'+attractorCSV+'.csv', sep = '\t')
