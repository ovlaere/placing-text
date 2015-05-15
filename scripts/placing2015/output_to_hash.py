import sys
import bz2

def main():

	if len(sys.argv) != 3:
		print "Missing arguments: test_input test_placing"
		exit(1)

	# Create a mapping
	mapping = {}

	# Open the file with original test data
	datafile = open(sys.argv[1], 'r') 
	# skip the line with the line count
	datafile.readline()
	# Loop through the data file we want to map
	for line in datafile:
		# Split the line on comma
		pieces = line.rstrip().split(",")
		# Add the id and hash
		mapping[pieces[0]] = pieces[1]
	# close the file
	datafile.close()

	# Open the output file
	outputfile = open(sys.argv[2], 'r') 
	# Loop through the output file
	for line in outputfile:
		# Split the line on tab
		pieces = line.rstrip().split(" ")
		
		id = pieces[0]
		lat = pieces[1]
		lon = pieces[1]

		print "%s;%s;%s" % (mapping[id], lat, lon)

	# close the file
	outputfile.close()

	# All done, all data going to stdout

if __name__ == '__main__':
	main()