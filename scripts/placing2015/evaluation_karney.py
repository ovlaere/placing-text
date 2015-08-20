import sys
import numpy
import bz2
from geographiclib.geodesic import Geodesic

def main():

	if len(sys.argv) < 3:
		print "Missing arguments: ground_truth_file.bz2 [file_to_evaluate ..]"
		exit(1)

	print "Loading ground truth..."
	coord_map = {}
	# Open the file with data to process
	ground_truth = bz2.BZ2File(sys.argv[1], 'r') 
	for line in ground_truth:
		pieces = line.rstrip().split(",")
		if len(pieces) == 3:
			hash = pieces[0]
			lat = float(pieces[1])
			lon = float(pieces[2])
			coord_map[hash] = { 'lat' : lat, 'lon' : lon}
		
	ground_truth.close()
	print "Done. Loaded: %s items" % len(coord_map)
	
	for filename in sys.argv[2:]:

		print "%s --> evaluating " % (filename)
		errors = 0
		counter = 0
		distances = []
		evalfile = open(filename, 'r')
		outputfile = open(filename + ".evaluation.tsv", 'w')
		thresholds = [0.001,0.01,0.1,1,10,100,1000,10000,40000]
		counters = [0,0,0,0,0,0,0,0,0]

		# Write header to file
		header = ['hash', 'estimated_lat', 'estimated_lon', 'real_lat', 'real_lon', \
		'error_km']
		outputfile.write("\t".join(map(str, header)) + "\n")

		for line in evalfile:
			try:
				pieces = line.rstrip().split(";")
				hash = pieces[0]
				lat = float(pieces[1])
				lon = float(pieces[2])
				# Get ground truth coords
				g_lat = coord_map[hash]['lat']
				g_lon = coord_map[hash]['lon']
				distance = Geodesic.WGS84.Inverse(lat, lon, g_lat, g_lon)
				distance_meter = distance['s12']
				distances.append(distance_meter)
				for i in range(0,len(thresholds)):
					if (distance_meter/1000) < thresholds[i]:
						counters[i] += 1

				outputfile.write("\t".join(map(str, \
					[hash, lat, lon, g_lat, g_lon, (distance_meter / 1000)])) + "\n")

				counter += 1
				# Print stats
				if counter % 25000 == 0:
					print "%s --> evaluating (%s %%) " % \
						(filename, (counter*100.0/len(coord_map)))

			except Exception, e:
				print "Error during parsing: %s" % (e)
				print line
				errors += 1
				outputfile.close()
				sys.exit(1)



		outputfile.write("\n")
		outputfile.write("distance\t#items\tpercentage"+"\n")
		for i in range(0,len(thresholds)):
			outputfile.write("%s km\t%s\t%s %%" % (thresholds[i], counters[i], \
				"{:10.2f}".format(counters[i]*100.0/counter))+"\n")

		# min, max, stddev, average, median
		np_arr = numpy.array(distances)
		str_min = "{:10.4f}".format(numpy.min(np_arr)/1000)
		str_max = "{:10.4f}".format(numpy.max(np_arr)/1000)
		str_avg = "{:10.4f}".format(numpy.average(np_arr)/1000)
		str_std = "{:10.4f}".format(numpy.std(np_arr)/1000)
		str_q1 = "{:10.4f}".format(numpy.percentile(np_arr, 25)/1000) 
		str_q2 = "{:10.4f}".format(numpy.percentile(np_arr, 50)/1000) 
		str_q3 = "{:10.4f}".format(numpy.percentile(np_arr, 75)/1000)

		outputfile.write("min:\t%s\n" % (str_min))
		outputfile.write("max:\t%s\n" % (str_max))
		outputfile.write("mean:\t%s\n" % (str_avg))
		outputfile.write("stddev:\t%s\n" % (str_std))
		outputfile.write("Q1:\t%s\n" % (str_q1))
		outputfile.write("Q2:\t%s\n" % (str_q2))
		outputfile.write("Q3:\t%s\n" % (str_q3))
		outputfile.close()

		print "%s --> %s " % (filename, "Q1:\t%s" % (str_q1))
		print "%s --> %s " % (filename, "Q2:\t%s" % (str_q2))
		print "%s --> %s " % (filename, "Q3:\t%s" % (str_q3))

	# All done

if __name__ == '__main__':
	main()
