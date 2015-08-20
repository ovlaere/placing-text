MediaEval2015 Placing Task LOCALE sub-task Baseline
===================================================

# Introduction

This branch of [placing-text][placing-text] publishes the code used to run a baseline for the [MediaEval 2015][MediaEval] Placing Task **LOCALE** sub-task. This year's task consists of 4 695 149 training photos and videos, while the test set contains 949 889 items. The data for this sub-task is a subset of the [Yahoo Flickr Creative Commons 100M dataset][YFCC100M]. The hashes of the photos and videos to be used from the dataset have been distributed among the participants to the [Placing Task][Placing].

In this branch, we provide scripts to extract the training and test set from the provided datasets and reproduce baseline geopredictions using the framework from [placing-text][placing-text]. Please note that this approach uses textual features only, and does not use any visual features at all.

# Reproducing the locale subtask baseline results

To reproduce or verify the results from the baseline published by the 2015 Placing Task organisation, you can take the steps described below. All code was run on a Mid 2014 Macbook Pro, with a 2.2GHz Intel Core i7 processor, 16GB 1600MHz DDR3 memory and a SSD drive. All timings are relative to that system. For details about certain steps, we recommend you take a look at the actual source code of the scripts.

Note: the Python code reportedly only works on version 2.x (which was used in development), it is untested on version 3.

## Preparing the input data

	# Clone the repo
	git clone git@github.com:ovlaere/placing-text.git
	# Go into the folder
	cd placing-text
	# Fetch all branches
	git fetch —all
	# Show branches (for info purpose)
	git branch -va
	# Checkout the mediaeval2015 branch
	git checkout -b mediaeval2015 origin/mediaeval2015

Next, make sure you have the [Yahoo Flickr Creative Commons 100M dataset][YFCC100M] downloaded into a folder on your system. We will refer to this in the next steps as the `YFCC100M_FOLDER`. Likewise, you should have the 2015 Placing Task data in a folder, to which we will refer as the `PT2015_FOLDER`.

Now, we need to extract a mapping from photo and video hashes (from the PT dataset) to IDs (to be able to extract them from the YFCC100M dataset). To this end, run the following python scripts:

	# extract a mapping from hash to id for test data
	python ./scripts/placing2015/hash_to_id.py \
	PT2015_FOLDER/mediaeval2015_placing_locale_test.bz2 \
	YFCC100M_FOLDER/yfcc100m_hash.bz2 > data/placing2015/test_data.txt

	# extract a mapping from hash to id for training data
 	python ./scripts/placing2015/hash_to_id.py \
 	PT2015_FOLDER/mediaeval2015_placing_locale_train.bz2 \
 	YFCC100M_FOLDER/yfcc100m_hash.bz2 > data/placing2015/train_data.txt

This takes around ** 8 minutes ** per file on the reference system, and can be run in two terminals next to each other.

Next, we use these two mapping files to extract the actual training and test data into the placing-text format. You can write custom parsers for any input format, but to make our lives easy, we just format the output according to the default format:

	<number_of_items>
    ID,hash,latitude,longitude,tag tag tag tag
    ID,hash,latitude,longitude,tag tag tag tag
    ...
    ID,hash,latitude,longitude,tag tag tag tag

That is, a line with the number of items in the file, and then for each line an ID, the hash field from the YFCC100M dataset as a reference, the latitude and longitude of the
item (in the case of training data) and a space-separated field of tags.

	# extract the actual test data based on the mapping file
	python ./scripts/placing2015/build_test.py data/placing2015/test_data.txt \
	YFCC100M_FOLDER/yfcc100m_dataset-* > data/placing2015/test.orig

	# extract the actual training data based on the mapping file
	python ./scripts/placing2015/build_train.py data/placing2015/train_data.txt \
	YFCC100M_FOLDER/yfcc100m_dataset-* > data/placing2015/training.orig

This process will take around ** 60 minutes ** per file on the reference system, and is highly recommended to run in two terminals next to each other.

A final step to prepare the input data, is to normalize the tags (i.e. cleaning up a bit). To this end, run:

	# Run the normalizer against the test data
	java -cp target/classes org.multimediaeval.placing.Normalizer \
	data/placing2015/test.orig data/placing2015/test

	# Run the normalizer against the training data
	java -cp target/classes org.multimediaeval.placing.Normalizer \
	data/placing2015/training.orig data/placing2015/training

This last step only takes one or two minutes. At this point, you have both `data/placing2015/test` and `data/placing2015/training` in place, which is the input data for the 2015 Placing Task.

## Running the georeferencing process

Please note that we will immediately set the memory to 8 GB of ram, with maximum heap size of 16GB. If you have less than 16GB at your disposal, adjust these settings accordingly. Also, you will have to set the variable `memory_available_in_gb` in `org.multimediaeval.placing.MediaEval2015PlacingLocale` on line 101 to the correct value.

Once ready to run, execute the following:

	# Package the project
	mvn package
	# Run the georeferencer
	java -cp target/placing-text-0.0.1-SNAPSHOT-jar-with-dependencies.jar \
    -Xms8g -Xmx16g -ea -Dfile.encoding=UTF-8 \
    org.multimediaeval.placing.MediaEval2015PlacingLocale

This workflow will cluster the training data (4.7M points) into 3 different clustering levels (500, 2500 and 10000 clusters). On the reference system, this step took 6 hours and 8 minutes. To facilitate reproduction, we uploaded the resulting clustering files into the `data/placing2015` folder. This not only removes the burden of computing these, it also allows exact reproduction of the results.

Once the clustering is done, the workflow will train a Naive Bayes classifier for these 3 clusterings. The georeferencing process will do a similarity search over all training data, which can take some time. In the end, the platform delivers the file `data/placing2015/test.placing`, which contains the location predictions for the test data.

Excluding clustering, this whole process took **45 minutes and 38 seconds** on the reference system.

## Formatting the output for submission

The georeferencer will output the data using IDs instead of hashes. The final step is to map these IDs back to the hashes from the original data, and format the file in the required format for submission. For this, we run the following script:

	# Remap the output to hashes, rename and format for submission
	python ./scripts/placing2015/output_to_hash.py data/placing2015/test \
	data/placing2015/test.placing > data/placing2015/me15pt_[subtask]_[group]_[run].txt

The file `data/placing2015/me15pt_[subtask]_[group]_[run].txt` will then contain a valid submission.

## Evaluation

In the folder `scripts/placing2015` you can find the code for generating the distance based evaluation results using Karney's distance. This requires the `geographiclib` for Python and `numpy`. If you have the required modules installed, you can evaluate a submission for the mobility or locale task using the command:

	python ./scripts/placing2015/evaluation_karney.py data/placing2015/ground.[locale|mobility].csv.bz2 \
	<submission_file> <submission_file> ...

The script will generate in the same folder as `submission_file` a file called `submission_file.evaluation.tsv` with the results.

## Questions/Issues

If you have any problems using this code, just let me know via oliviervanlaere@gmail.com.

## References

The code run for this baseline is based off the optimized submission of the Gent - Cardiff Univeristy team submission to the 2012 Placing Task. It was part of my PhD dissertation about [Georeferencing Text Using Social Media][PhD]. The code in this framework covers certain aspects that have been published in 
scientific papers. Therefore, if you use the code from this framework, please
cite the following paper(s):

* [Georeferencing Flickr resources based on textual meta-data][INS]. Olivier Van 
Laere, Steven Schockaert, Bart Dhoedt. 

In case you would use or refer to the spatially aware method of feature ranking
based on the Ripley K implementation, please cite the following paper:

* [Spatially-Aware Term Selection for Geotagging][INS]. Olivier Van Laere, 
Jonathan Quinn, Steven Schockaert, Bart Dhoedt.

In case you make use of the geographical spread score feature ranking method, 
please cite the authors of this scoring method:

* [WISTUD at MediaEval 2011: Placing Task][WISTUD]. Claudia Hauff and 
Geert-Jan Houben.

[PhD]: http://www.van-laere.net/Phd_VanLaereOlivier.pdf  "Georeferencing Text Using Social Media"
[INS]: http://dx.doi.org/10.1016/j.ins.2013.02.045 "Georeferencing Flickr resources based on textual meta-data"
[TKDE]: http://www.computer.org/csdl/trans/tk/preprint/06475942-abs.html "Spatially-Aware Term Selection for Geotagging"
[WISTUD]: http://ceur-ws.org/Vol-807/Hauff_WISTUD_Placing_me11wn.pdf "WISTUD at MediaEval 2011: Placing Task"
[placing-text]: https://github.com/ovlaere/placing-text "placing-text"
[MediaEval]: http://www.multimediaeval.org "MediaEval 2015"
[Placing]: www.multimediaeval.org/mediaeval2015/placing2015/ "MediaEval Placing Task"
[YFCC100M]: http://bit.ly/yfcc100md "Yahoo Flickr Creative Commons 100M dataset"
