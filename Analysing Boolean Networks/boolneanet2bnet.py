import pystablemotifs.format as format

with open('output_boolNet_booleanNet.txt') as in_file:
    lines = in_file.readlines()

with open('somefile.txt', 'a') as the_file:
        for line in lines:
            the_file.write(format.booleannet2bnet(line)+'\n')
