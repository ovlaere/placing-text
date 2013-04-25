General info
============

This project provides the framework for automated georeferencing of textual 
(meta)data. This framework was developed during for PhD dissertation (at Ghent 
University, Belgium) [Georeferencing Text Using Social Media][PhD].

The code in this framework covers certain aspects that have been published in 
scientific papers. Therefore, if you use the code from this framework, please
cite the following paper:
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

This code has been used to participate in the 2010, 2011 and 2012
[MediaEval Placing Task][Placing]. Therefore, an example is also provided on how to 
run a baseline submission for this task. This framework uses only textual meta-
data, no visual features are used.

How to get started
==================

The code in this framework expects two files to be present for you 
georeferencing problem: a training and a test file. The file format is up to you
as you can implement your own parsers. The examples however use a parser that
expects these files to have a format such as 

    <number_of_items>
    ID,...,latitude,longitude,tag tag tag tag
    ID,...,latitude,longitude,tag tag tag tag
    ...
    ID,...,latitude,longitude,tag tag tag tag

That is, and ID, an obsolete field (in my case - flickr data - that was the 
owner which is unused in this implementation), the latitude and longitude of the
item and a space-separated field of tags.

**Important:** To increase performance, the framework tries to read the number of 
lines in the training and test file from the first line. If you put the number
of items in that line, this is parsed and used in the code. If you forget this
or omit this on purpose, the framework will loop through the file to determine
the number of lines in the file. When using training files consisting of for
instance 32 million lines, this might decrease your performance.

Most parts of the code are parallelized for execution on multiple cores, so the
more cores you have on the system running this code, the better.

As for memory requirements: the language models are build in memory, so the more
memory you can spare the virtual machine (-Xmx parameter), the better. If you
try to run a model with lots of classes and lots of features, the code will run
through it in batches (e.g. 1000 classes a time using 1M features), while more
memory or less features would allow you to process 3000 classes per batch. 
Anyway, this is nothing to worry about at this point. Just start running the 
examples and see where you get.

The workflow to obtain location predictions for a set of test items, starting
from the training data, is the following:

1. The training data is clustered.
2. The training data is analyzed for features that are ranked (one way or 
another, preferably ranked according to the geographic clues they provide)
3. A Naive Bayes classifier is trained, using the classes (clusters) discovered
and a given number of features.
4. The classifier returns a class that is most likely for each test item.
5. For each of these test items, the training data within their predicted class
is analyzed for the most similar training item. The location of this item is then
returned as the location estimation.
6. The predicted locations are compared to the ground truth after which detailed
statistics are presented about the results over the entire test collection.

For each of these steps, a documented example is available. Also, take a look in
the classes used by these examples, as they are all well documented.

If you would like to implement parsers for your own input files, have a look at 
the interface definitions in `be.ugent.intec.ibcn.geo.common.interfaces`. 
Different implementations can be found in 
`be.ugent.intec.ibcn.geo.common.io.parsers`.

Examples
--------

* Clustering      `be.ugent.intec.ibcn.examples.ClusteringExample`
* Feature ranking `be.ugent.intec.ibcn.examples.FeatureExample`
* Classification  `be.ugent.intec.ibcn.examples.Classifier`
* Georeferencing  `be.ugent.intec.ibcn.examples.ReferencingExample`
* Analyzing       `be.ugent.intec.ibcn.examples.AnalyzerExample`

Additionally, the following example provides a workflow that combines all 
necessary steps to end with a file that predicts the locations for a given test
collection from the MediaEval Placing Task, given a certain training file.

* MediaEval 2012 Placing Task workflow `be.ugent.intec.ibcn.examples.MediaEval2012PlacingExample`

Implementation details
======================

* Clustering algorithms
    * Grid clustering
    * Partitioning Around Medoids
* Feature ranking
    * Chi Square
    * Max-Chi Square
    * Information Gain
    * Log-Likelihood
    * Most Frequently Used
    * Geographic spread score
    * Ripley-K based spatially aware ranking
* Location prediction
    * Medoid based conversion
    * Similarity based conversion (Jaccard)

External libraries
------------------

This code makes extensive use of KD-trees, for which we would like to 
acknowledge the used implementation from 
http://home.wlu.edu/~levys/software/kd/.

[PhD]: http://www.van-laere.net/Phd_VanLaereOlivier.pdf  "Georeferencing Text Using Social Media"
[INS]: http://dx.doi.org/10.1016/j.ins.2013.02.045 "Georeferencing Flickr resources based on textual meta-data"
[TKDE]: http://www.computer.org/csdl/trans/tk/preprint/06475942-abs.html "Spatially-Aware Term Selection for Geotagging"
[Placing]: http://www.multimediaeval.org/mediaeval2012/placing2012/index.html "MediaEval Placing Task"
[WISTUD]: http://ceur-ws.org/Vol-807/Hauff_WISTUD_Placing_me11wn.pdf "WISTUD at MediaEval 2011: Placing Task"