#!/bin/python 
"""
build a lexicon of the features (paths) which occur with at least J unique noun pairs.
saves as lexicons, one for each path type, into files called {basic, collapsed, propagated}.lexicon.
"""
import sys
from sys import stderr

#NPs per path
J = 5	

#keys are NPs, values are dictionaries of path : count
bpaths = dict()
ppaths = dict()
cpaths = dict()
#maps NP to label (independent, hnym, etc)
labels = dict()
#keys are paths, values are counts
bpath_counts = dict()
cpath_counts = dict()
ppath_counts = dict()

stderr.write('Counting\n')
for line in sys.stdin:
	p1, p2, p3, label, x, y = line.strip().split('\t')
	key = '%s|||%s'%(x,y)
	if key not in labels: 
		labels[key] = (label)
		bpaths[key] = dict()
		cpaths[key] = dict()
		ppaths[key] = dict()
	#keep track of NPs with which each path appears
	if p1 not in bpath_counts: bpath_counts[p1] = 0
	if p2 not in cpath_counts: cpath_counts[p2] = 0
	if p3 not in ppath_counts: ppath_counts[p3] = 0
	#Only count unique NPs for each path
	if p1 not in bpaths[key]: bpath_counts[p1] += 1
	if p2 not in cpaths[key]: cpath_counts[p2] += 1
	if p3 not in ppaths[key]: ppath_counts[p3] += 1
	#Keep track of number of times each NP occurs with each path
	#NOT CURRENTLY USED IN THIS SCRIPT	
	if p1 not in bpaths[key] : bpaths[key][p1] = 0
	if p2 not in cpaths[key] : cpaths[key][p2] = 0
	if p3 not in ppaths[key] : ppaths[key][p3] = 0
	bpaths[key][p1] += 1
	cpaths[key][p2] += 1
	ppaths[key][p3] += 1

stderr.write('Filtering Lexicon\n')
#Filter lexicon to only contain paths which occur with at least J NPs
blex = [p for p in bpath_counts if bpath_counts[p] >= J]
clex = [p for p in cpath_counts if cpath_counts[p] >= J]
plex = [p for p in ppath_counts if ppath_counts[p] >= J]

bout = open('lexicon.basic', 'w')
cout = open('lexicon.collapsed', 'w')
pout = open('lexicon.propagated', 'w')

for f in blex:
	bout.write('%s\n'%f)
bout.close()
for f in clex:
	cout.write('%s\n'%f)
cout.close()
for f in plex:
	pout.write('%s\n'%f)
pout.close()
	
