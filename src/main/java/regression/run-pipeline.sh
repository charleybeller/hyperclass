#!/bin/bash

#$ -cwd 
#$ -l h_vmem=100g,mem_free=100g,h_rt=550:00:00
#$ -S /bin/bash
#$ -M elliepavlick@gmail.com
#$ -m eas
#$ -j y -o logs/

# Load your bashrc file
. ~/.bashrc

#INDIR=demo
INDIR=/export/common/SCALE13/Text/u/beller/satellite/ 
#OUTDIR=models/demo
OUTDIR=models/0814
MODELDIR=$OUTDIR/basic
REPO=../../../../lib

mkdir $OUTDIR/data
mkdir $OUTDIR/basic
mkdir $OUTDIR/collapsed
mkdir $OUTDIR/propagated

#take data from input directory and write to single file, ignoring noun pairs for which no definitive relation could be found.
python scripts/preprocess_allsplits.py $INDIR > $OUTDIR/data/data.wn
cat $OUTDIR/data/data.wn | sort | uniq > $OUTDIR/data/data.wn.uniq

#extract lexicon of paths which occur with at least 5 unique NPs
cat $OUTDIR/data/data.wn.uniq | python scripts/feature-lexicon.py

#collapse to one NP per line, filter out paths which appear with fewer than 5 unique NPs, and filter out NPs with fewer than 5 unique paths
cat $OUTDIR/data/data.wn.uniq | python scripts/path-features-in-lexicon.py > $OUTDIR/data/data.wn.uniq.type

#split into train, dev, test
sh scripts/split_traintest.sh $OUTDIR/data/data.wn.uniq.type $OUTDIR/data/

#split into files by path type
cat $OUTDIR/data/train | python scripts/split_by_path.py $OUTDIR data.raw
cat $OUTDIR/data/dev | python scripts/split_by_path.py $OUTDIR dev.raw

#train the model and test the regression
java -cp $REPO/mallet.jar:$REPO/mallet-deps.jar:. FullDataReader $MODELDIR
java -cp $REPO/mallet.jar:$REPO/mallet-deps.jar:. RunFullRegression $MODELDIR

