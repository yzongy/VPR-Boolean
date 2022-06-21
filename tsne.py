# Load Python Libraries
import swat
import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns
from time import time
import matplotlib
import numpy
from numpy import genfromtxt
import csv
from sklearn.manifold import TSNE
from sklearn.decomposition import PCA

def tsne_2D(data, y, title):
    # data = [[1,2,3....,n],[3,2,1...,n],........[2,2,1,.....,n]]
    # y = [1, 0, 1, 1, 0, 2, 2, 1]

    palette = sns.color_palette("bright", 10)#takes dead strains into account

    embedded = TSNE(n_components=2, perplexity=40, early_exaggeration=12).fit_transform(data)
    convertCSV(embedded,"dirEvo_dataset_all5Rounds_plusCRISPRi")
    filled_markers = ('o', 'v', '^', '<', '>', '8', 's', 'p', '*', 'h', 'H', 'D', 'd', 'P', 'X')
    sns.scatterplot(embedded[:,0], embedded[:,1], hue=y, style=y, palette=palette, legend='full', alpha=0.45, markers=filled_markers)
    plt.title(title)
    plt.xlabel('Dimension 1')
    plt.ylabel('Dimension 2')
    plt.show()

def tsne_3D(data, FitRank, title):
    from matplotlib import pyplot
    from mpl_toolkits.mplot3d import Axes3D
    fig = pyplot.figure()
    ax = Axes3D(fig)
    p = 15
    embedded = TSNE(n_components=3, perplexity=p, early_exaggeration=12).fit_transform(data)
    ax.scatter(embedded[:,0], embedded[:,1], embedded[:,2],c=FitRank,s=[abs(n-1) for n in FitRank])#get rid of n=1(wild type fitness)
    fig.suptitle(title + ' perplexity=' +str(p), fontsize=16)
    pyplot.show()

def threeDimensionPlot(data,fitness,title):
    from matplotlib import pyplot
    from mpl_toolkits.mplot3d import Axes3D
    import matplotlib.cm as cmx

    colorsMap='seismic'
    cm = plt.get_cmap(colorsMap)
    cNorm = matplotlib.colors.Normalize(vmin=min(fitness), vmax=max(fitness))
    scalarMap = cmx.ScalarMappable(norm=cNorm, cmap=cm)

    fig = plt.figure(figsize=(8, 6))
    ax = fig.add_subplot(111, projection='3d')
    embedded = TSNE(n_components=3, perplexity=10, early_exaggeration=12).fit_transform(data)
    ax.scatter(embedded[:,0],embedded[:,1], embedded[:,2],c=scalarMap.to_rgba(fitness),s=size(fitness))
    scalarMap.set_array(fitness)
    fig.colorbar(scalarMap)
    ax.set_xlabel('Dimension 1')
    ax.set_ylabel('Dimension 2')
    ax.set_zlabel('Dimension 3')
    fig.suptitle(title, fontsize=16)
    plt.show()

def size(fitness):
    size = []
    for n in fitness:
        if n < 0:
            size.append(abs(n)*10)
        else:
            size.append(0)
    return size

def runPCA(data,y,title):
    pca = PCA(n_components=2)
    pcaResults = pca.fit_transform(data)
    palette = sns.color_palette("bright", 3)
    sns.scatterplot(pcaResults[:,0], pcaResults[:,1], hue=y, style=y, palette=palette, legend='full', alpha=0.45)
    plt.title(title)
    plt.xlabel('Dimension 1')
    plt.ylabel('Dimension 2')
    plt.show()

def convertCSV(embedded,title):
        #embedded = tsne(data)
        xarray = []
        yarray = []
        for row in embedded:
            xarray.append(row[0])
            yarray.append(row[1])
        numpy.savetxt(title+'_tsne_Xaxis.csv', xarray, delimiter=",")
        numpy.savetxt(title+'_tsne_Yaxis.csv', yarray, delimiter=",")

def main():
    # Store your data in the variable data as a 2D array
    X = pd.read_csv("dirEvo_dataset_all5RoundsCopyNum_plusCRISPRi.csv", delimiter=',')
    # Store your labels in the variable labels as a list
    yreader = csv.reader(open("dirEvo_dataset_rounds_plusCRISPRi.csv","rt"))
    y = list(yreader)
    StrainRank_data = numpy.array(y).astype("int")
    FitRank = [ ]
    for rank in StrainRank_data:
       FitRank.extend(rank)


    # fitScoreReader = csv.reader(open("4658_BinaryData_FitScore_withDeadStrs.csv","rt"))
    # fs = list(fitScoreReader)
    # fitScore_data = numpy.array(fs).astype("float")
    # fitScore = [ ]
    # for score in fitScore_data:
    #     fitScore.extend(score)
    #Run tsne
    tsne_2D(X, FitRank, 'Directed Evolution 5 Rounds')
    #threeDimensionPlot(X,fitScore,'t-SNE of 3D SCRaMbLE Simulation')
    #tsne_3D(X,FitRank,'t-SNE of SCRaMbLE Simulation')
    #runPCA(X,FitRank,'PCA of 10%FBA')
    exit()


if __name__ == '__main__':
    main()
