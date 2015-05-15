import sys
import bz2

def main():

	if len(sys.argv) != 3:
		print "Missing arguments: hash_file.bz2 hash_to_id_mapping.bz2"
		exit(1)

	# Open the file with data to process
	id_file = bz2.BZ2File(sys.argv[1], 'r') 
	# Open the file with hashes to ids
	hash_file = bz2.BZ2File(sys.argv[2], 'r') 

	# Create a mapping
	mapping = {}

	# keep track of a set of ids to process
	to_process = set()
	# Loop through the hash file we want to map
	for line in id_file:
		# Split the line on tab
		pieces = line.rstrip().split("\t")
		# Add the id which is in the first column
		to_process.add(pieces[0])
	# close the file
	id_file.close()

	# Loop through the overall hash file
	for line in hash_file:
		# Split the line on tab
		pieces = line.rstrip().split("\t")
		
		id = pieces[0]
		hash = pieces[1]

		# If the has is in the list to process
		if hash in to_process:
			# Add it to the mapping
			mapping[hash] = id
	# close the file
	hash_file.close()

	# We have the mapping now

	# print the number of lines for the file header
	print len(to_process)

	# Re-open the file with data to process
	id_file = bz2.BZ2File(sys.argv[1], 'r') 
	# Loop through the hash file we want to map
	for line in id_file:
		# Split the line on tab
		pieces = line.rstrip().split("\t")
		# Fetch the hash
		hash = pieces[0]
		# Fetch the mapping, set -1 in case of error
		id = mapping.get(hash, -1)
		if id > 0:
			print "%s\t%s" % (id, line.rstrip())
		# else:
		# 	print "Missing id for hash %s" % (hash)

	# close the file
	id_file.close()

	# All done, all data going to stdout

if __name__ == '__main__':
	main()