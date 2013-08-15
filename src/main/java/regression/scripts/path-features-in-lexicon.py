#!/bin/python 

import sys
from sys import stderr

"""
Filter out examples to only include NPs which occur with at least K unique features from the lexicon.
For info on lexicon, see feature-lexicon.py.
"""
#paths per NP
K = 5	

bpaths = dict()
ppaths = dict()
cpaths = dict()
labels = dict()
bpath_counts = dict()
cpath_counts = dict()
ppath_counts = dict()

#Filter lexicon to only contain paths which occur with at least J NPs
blex = set() 
clex = set() 
plex = set() 
for p in open('lexicon.basic').readlines():
	p = p.strip()
	blex.add(p)
for p in open('lexicon.collapsed').readlines():
	p = p.strip()
	clex.add(p)
for p in open('lexicon.propagated').readlines():
	p = p.strip()
	plex.add(p)

for line in sys.stdin:
	p1, p2, p3, label, x, y = line.strip().split('\t')
	#take just the word, not the lemma
	x = x.split('|')[0]
	y = y.split('|')[0]
	key = '%s|||%s'%(x,y)
	if key not in labels: 
		labels[key] = (label)
		bpaths[key] = dict()
		cpaths[key] = dict()
		ppaths[key] = dict()
	#Keep track of number of times each NP occurs with each path	
	if p1 in blex: 
		if p1 not in bpaths[key] : bpaths[key][p1] = 0
		bpaths[key][p1] += 1
	if p2 in clex: 
		if p2 not in cpaths[key] : cpaths[key][p2] = 0
		cpaths[key][p2] += 1
	if p3 in plex: 
		if p3 not in ppaths[key] : ppaths[key][p3] = 0
		ppaths[key][p3] += 1

n = len(labels)
for i,key in enumerate(labels.keys()):
	x,y = key.split('|||')
	label = labels[key]
	#colons interfere with Mallets feature:value format
	bplist = ['%s:%d'%(p.replace(':','|'), bpaths[key][p]) for p in bpaths[key]]
	cplist = ['%s:%d'%(p.replace(':','|'), cpaths[key][p]) for p in cpaths[key]]
	pplist = ['%s:%d'%(p.replace(':','|'), ppaths[key][p]) for p in ppaths[key]]

	#Take only NPs with at least K unique paths (according to the propagated paths)
	if len(pplist) >= K:
		if len(bplist) > 0 : bp = ','.join(bplist)
		else: bp = 'None'
		if len(cplist) > 0 : cp = ','.join(cplist)
		else: cp = 'None'
		if len(pplist) > 0 : pp = ','.join(pplist)
		else: pp = 'None'
		print '%s\t(%s,%s)\t%s\t%s\t%s'%(label, x, y, bp, cp, pp)

	
