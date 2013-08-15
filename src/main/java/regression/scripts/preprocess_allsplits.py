#!bin/python 
"""
read in output of Hyperclass.scala, filter out OOV/unknown nounpairs, and write into format with single label per example
"""

import sys
import os

dir = sys.argv[1]

#format of line  : n:nn:n	n:nn:n	n:nn:n	OOV	unknown	unknown	unknown	unknown	unknown	X=3rd	Y=Rd	phrases=3rd Rd 

fmtstr = '%s\t%s\t%s\t%s\t%s\t%s'

for split in os.listdir(dir):
	for file in os.listdir('%s/%s'%(dir,split)):
	        for line in open("%s/%s/%s"%(dir, split, file)):
			comps = line.strip().split('\t')
			if comps[3] != 'OOV': #if both words are in wordnet
				labels = comps[4:9]
				if labels.count('possible') > 1 : continue #if label is ambiguous, multiple possible labels
				issyn = (labels[0] == 'synonym') or (labels[0] == 'possible')
				ishyper = (labels[1] == 'hypernym')or (labels[1] == 'possible')
				ishypo = (labels[2] == 'hyponym') or (labels[2] == 'possible')
				isant = (labels[3] == 'antonym') or (labels[3] == 'possible')
				isalt = (labels[4] == 'sibling') or (labels[4] == 'possible') #use conservative definition of alternation
				isntsyn = (labels[0] == 'nonsynonym') 
				isnthyper = (labels[1] == 'nonhypernym') 
				isnthypo = (labels[2] == 'nonhyponym') 
				isntant = (labels[3] == 'nonantonym') 
				isntalt = (labels[4] == 'alternation' or labels[4] == 'nonalternation') 
				if isntsyn and isnthyper and isnthypo and isntant and isntalt: 
					 print fmtstr%(comps[0], comps[1], comps[2], 'independent', comps[9], comps[10])
				if issyn: print fmtstr%(comps[0], comps[1], comps[2], 'synonym', comps[9], comps[10])
				if ishyper or ishypo: print fmtstr%(comps[0], comps[1], comps[2], 'hnym', comps[9], comps[10])
				if isant: print fmtstr%(comps[0], comps[1], comps[2], 'antonym', comps[9], comps[10])
				if isalt: print fmtstr%(comps[0], comps[1], comps[2], 'alternation', comps[9], comps[10])
	
	
