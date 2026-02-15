This package contains an Activity-oriented Future, which can semi-transparently upload large amounts of data to
external stores, effectively avoiding Cadence's per-event / per-workflow size limits.

Multiple Futures can be used in a single Activity's arguments or response values, e.g. in separate struct fields or
slices, but each Future will be unaware of the others.  Any max-size limit you choose will not be shared between all
Futures (they each have their own limit), so the cumulative size of your response may be much larger.
Take care to keep WithMaxBytes limits low enough for all values, and be aware that the URLs that replace the data
also take space - dozens are fine, thousands are probably not.

# Caveats

This tool comes with some semi-severe caveats, but if they are acceptable for your use, it may allow you to easily
reduce your Workflow's history's data use:

# Workflows will not have access to data

By design, this tool does not allow you to access wrapped data in your workflows.  You can use the type to forward data
between activities, but not inspect the contents - they may not exist, and you cannot safely perform the download in
your workflow.
