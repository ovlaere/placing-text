import sys
import bz2
import urllib
import datetime

def main():
	report_size = 100000
	SEPARATOR="DATA"
	if len(sys.argv) < 4:
		print "Missing arguments: inputfiles*.bz2 %s data_file*.bz2" % (SEPARATOR)
		exit(1)
	
	# Find the input files and the data files
	cutoff = None
	for i in range(1,len(sys.argv)):
		if sys.argv[i] == SEPARATOR:
			cutoff = i

	input_counters = []
	input_to_process = []
	input_mapping = []
	input_fds = []

	file_index = 0
	# For each of the input files to process
	for inputfile in sys.argv[1:cutoff]:
		print "%s Processing %s" % (datetime.datetime.now().isoformat(), inputfile)
		# keep track of a set of ids to process
		input_to_process.append(set())
		# let's keep track of the mapping as well
		input_mapping.append({})
		# Open the inputfile
		input_stream = bz2.BZ2File(inputfile, 'r')
		# Loop through the hash file we want to map
		for line in input_stream:
			# Split the line on tab
			pieces = line.rstrip().split("\t")
			# Add the id which is in the first column
			input_to_process[file_index].add(pieces[0])
			# Add hash and places info per item
			input_mapping[file_index][pieces[0]] = "%s,%s" % (pieces[1], pieces[4])
		# close the file
		input_stream.close()
		# Open a write file for each of the input files
		input_fds.append(open("%s.processed" % (inputfile), 'w'))
		# Write the line count
		input_fds[file_index].write("%s\n" % (len(input_to_process[file_index])))
		# Increment the file_index
		file_index += 1

	# For each of the data files to process
	for datafile in sys.argv[cutoff+1:]:
		# set up a stream
		data_stream = bz2.BZ2File(datafile, 'r')
		print "%s Processing %s" % (datetime.datetime.now().isoformat(), datafile)
		# Set up a counter
		counter = 0
		# process each line
		for line in data_stream:
			pieces = line.rstrip().split("\t")
			# Get the photo id
			photo_id = pieces[0]

			# Check this against all known inputs
			for index in range(0, len(input_to_process)):
				# if this is one of the IDs to process
				if photo_id in input_to_process[index]:

					photo_id_meta = input_mapping[index][photo_id].split(",")
					hash = photo_id_meta[0]
					
					places_encoded = photo_id_meta[1].replace(",", " ").strip()
							
					title = pieces[6].replace(",", " ")
					description = pieces[7].replace(",", " ")
					user_tags = pieces[8].replace(",", " ")
					machine_tags = pieces[9].replace(",", " ")
					
					tags = "%s %s %s %s %s" % (places_encoded, title, description, user_tags, machine_tags)
					# Hacky way of replacing multiple whitespaces :-)
					tags = ' '.join(tags.split())

					# For test data, this is null
					longitude = pieces[10]
					latitude = pieces[11]

					# requested output format - tags need later postprocessing
					# ID,...,latitude,longitude,tag tag tag tag
					input_fds[index].write("%s,%s,%s,%s,%s\n" % (photo_id, hash, latitude, longitude, tags.strip()))

			counter += 1
			if counter % report_size == 0:
				print "%s Processing %s : %s" % (datetime.datetime.now().isoformat(), datafile, counter)

		data_stream.close()
	
	# Close all open files
	for fd in input_fds:
		fd.close()

	# All done

if __name__ == '__main__':
	main()