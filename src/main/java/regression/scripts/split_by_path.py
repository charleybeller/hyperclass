#!/bin/python 

import sys

"""
write parallel files, corresponding to each path type
"""

root = sys.argv[1]
fname = sys.argv[2]
b = open('%s/basic/%s'%(root, fname), 'w')
p = open('%s/propagated/%s'%(root, fname), 'w')
c = open('%s/collapsed/%s'%(root, fname), 'w')

bpaths = dict()
ppaths = dict()
cpaths = dict()
labels = dict()

for line in sys.stdin:
	label, pair, basic, col, prop = line.strip().split('\t')
	if not(basic == 'None') : b.write('%s\t%s\t%s\n'%(label, pair, '\t'.join(basic.split(','))))
	if not(col == 'None') : c.write('%s\t%s\t%s\n'%(label, pair, '\t'.join(col.split(','))))
	if not(prop == 'None') : p.write('%s\t%s\t%s\n'%(label, pair, '\t'.join(prop.split(','))))

b.close()
c.close()
p.close()
	
