hacking on the xview dataset
===


### xview references
- http://xviewdataset.org
- https://github.com/DIUx-xView
- https://challenge.xviewdataset.org/baseline
- https://challenge.xviewdataset.org/tutorial
- https://github.com/ultralytics/xview-docker
- https://github.com/PlatformStories/train-cnn-classifier
- https://medium.com/picterra/the-xview-dataset-and-baseline-results-5ab4a1d0f47f

### dl4j references
- 

### spark references
- https://github.com/databricks/spark-deep-learning
- https://github.com/yahoo/TensorFlowOnSpark
- https://towardsdatascience.com/deep-learning-with-apache-spark-part-2-2a2938a36d35
- https://github.com/cloudmesh/example-project-nist-fingerprint-matching
- https://medium.com/linagora-engineering/making-image-classification-simple-with-spark-deep-learning-f654a8b876b8
- oshinko
  - https://github.com/radanalyticsio
  - https://www.youtube.com/watch?v=IrHTeco4r_Q

### other references
- red blood cell detection
  - https://gist.github.com/saudet/fb8a4d9544dc3c411b302ccd6bbf87e7
  - https://github.com/cosmicad/dataset
- YOLT
  - https://github.com/CosmiQ/yolt
  - https://medium.com/the-downlinq/you-only-look-twice-multi-scale-object-detection-in-satellite-imagery-with-convolutional-neural-38dad1cf7571
  - https://medium.com/the-downlinq/yolt-arxiv-paper-and-code-release-8b30d40d095b
- http://fxapps.blogspot.com/2018/01/detecting-objects-using-javafx-and-deep.html
- https://github.com/ultralytics
- https://playground.tensorflow.org
- https://developer.nvidia.com/digits
- https://blog.skymind.ai/building-a-production-grade-object-detection-system-with-skil-and-yolo/
- https://skymind.ai/wiki/convolutional-network

### datasets
- https://github.com/DIUx-xView/baseline/releases
- https://www.nist.gov/itl/iad/image-group/resources/biometric-special-databases-and-software
- https://github.com/cosmicad/dataset
- http://www.cvlibs.net/datasets/kitti/
- https://geodacenter.github.io/data-and-lab/

### xview class labels

|id|name|
|---|---|
|11|Fixed-wing Aircraft|
|12|Small Aircraft|
|13|Cargo Plane|
|15|Helicopter|
|17|Passenger Vehicle|
|18|Small Car|
|19|Bus|
|20|Pickup Truck|
|21|Utility Truck|
|23|Truck|
|24|Cargo Truck|
|25|Truck w/Box|
|26|Truck Tractor|
|27|Trailer|
|28|Truck w/Flatbed|
|29|Truck w/Liquid|
|32|Crane Truck|
|33|Railway Vehicle|
|34|Passenger Car|
|35|Cargo Car|
|36|Flat Car|
|37|Tank car|
|38|Locomotive|
|40|Maritime Vessel|
|41|Motorboat|
|42|Sailboat|
|44|Tugboat|
|45|Barge|
|47|Fishing Vessel|
|49|Ferry|
|50|Yacht|
|51|Container Ship|
|52|Oil Tanker|
|53|Engineering Vehicle|
|54|Tower crane|
|55|Container Crane|
|56|Reach Stacker|
|57|Straddle Carrier|
|59|Mobile Crane|
|60|Dump Truck|
|61|Haul Truck|
|62|Scraper/Tractor|
|63|Front loader/Bulldozer|
|64|Excavator|
|65|Cement Mixer|
|66|Ground Grader|
|71|Hut/Tent|
|72|Shed|
|73|Building|
|74|Aircraft Hangar|
|76|Damaged Building|
|77|Facility|
|79|Construction Site|
|83|Vehicle Lot|
|84|Helipad|
|86|Storage Tank|
|89|Shipping container lot|
|91|Shipping Container|
|93|Pylon|
|94|Tower|

### nvidia docker
- install nvidia-docker
  - https://github.com/NVIDIA/nvidia-docker/wiki/Installation-(version-2.0)#prerequisites
- install cuda
  - https://docs.nvidia.com/cuda/cuda-installation-guide-linux/index.html
  - https://developer.nvidia.com/cuda-downloads
- register for nvidia registry
  - https://ngc.nvidia.com/registry
- run containers with `nvidia-docker`
  - `nvidia-docker run --rm nvidia/cuda nvidia-smi`
- debug issues with
  - `nvidia-container-cli -k -d /dev/tty info`

### segmenting labels

The json is easier to work with when partitioned

`jq '[.features[] | select(.properties.image_id == "100.tif")]' xView_train.geojson`

`for f in *.tif; do echo $f; jq --arg f "$f" '[.features[] | select(.properties.image_id == $f)]' ../xView_train.geojson > $f.geojson; done`

