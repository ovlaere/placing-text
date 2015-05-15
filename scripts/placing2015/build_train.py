import sys
import bz2
import urllib

def main():

	if len(sys.argv) < 3:
		print "Missing arguments: id_hash_file data_file*.bz2"
		exit(1)

	# Open the file with data to process
	id_hash_file = open(sys.argv[1], 'r')
	# Print the number of lines to the output, it is needed in the datafile anyway
	print id_hash_file.readline().strip()

	# keep track of a set of ids to process
	to_process = set()
	# let's keep track of the mapping as well
	mapping = {}
	# Loop through the hash file we want to map
	for line in id_hash_file:
		# Split the line on tab
		pieces = line.rstrip().split("\t")
		# Add the id which is in the first column
		to_process.add(pieces[0])
		# Add hash and GADM info per item
		mapping[pieces[0]] = "%s,%s" % (pieces[1], pieces[5])

	# close the file
	id_hash_file.close()	

	# For each of the data files to process
	for datafile in sys.argv[2:]:
		# set up a stream
		data_stream = bz2.BZ2File(datafile, 'r')

		# process each line
		for line in data_stream:
			pieces = line.rstrip().split("\t")
			# Get the photo id
			photo_id = pieces[0]

			# if this is one of the IDs to process
			if photo_id in to_process:

				photo_id_meta = mapping[photo_id].split(",")
				hash = photo_id_meta[0]
				
				gadm = photo_id_meta[1].replace("@", "|")
				gadm_pieces = gadm.split("|")
				gadm_encoded = ""
				for item in gadm_pieces:
					gadm_encoded += urllib.quote_plus(item) + " "
						
				gadm_encoded = gadm_encoded.replace(",", " ").strip()

				title = pieces[6].replace(",", " ")
				description = pieces[7].replace(",", " ")
				user_tags = pieces[8].replace(",", " ")
				machine_tags = pieces[9].replace(",", " ")
				
				tags = "%s %s %s %s %s" % (gadm_encoded, title, description, user_tags, machine_tags)
				# Hacky way of replacing multiple whitespaces :-)
				tags = ' '.join(tags.split())

				# For test data, this is null
				longitude = pieces[10]
				latitude = pieces[11]

				# requested output format - tags need later postprocessing
				# ID,...,latitude,longitude,tag tag tag tag
				print "%s,%s,%s,%s,%s" % (photo_id, hash, latitude, longitude, tags.strip())

		data_stream.close()

	# All done, all data going to stdout

if __name__ == '__main__':
	main()