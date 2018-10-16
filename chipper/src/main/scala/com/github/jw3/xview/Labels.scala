package com.github.jw3.xview

object Labels {
  sealed trait Label extends Cat {
    def id: Int
    def name: String
  }

  // small

  case object PassengerVehicle extends Label with CatSmall { val id: Int = 17; val name: String = "Passenger Vehicle" }
  case object SmallCar extends Label with CatSmall { val id: Int = 18; val name: String = "Small Car" }
  case object Bus extends Label with CatSmall { val id: Int = 19; val name: String = "Bus" }
  case object PickupTruck extends Label with CatSmall { val id: Int = 20; val name: String = "Pickup Truck" }
  case object UtilityTruck extends Label with CatSmall { val id: Int = 21; val name: String = "Utility Truck" }
  case object Truck extends Label with CatSmall { val id: Int = 23; val name: String = "Truck" }
  case object CargoTruck extends Label with CatSmall { val id: Int = 24; val name: String = "Cargo Truck" }
  case object TruckTractor extends Label with CatSmall { val id: Int = 26; val name: String = "Truck Tractor" }
  case object Trailer extends Label with CatSmall { val id: Int = 27; val name: String = "Trailer" }
  case object TruckWithFlatbed extends Label with CatSmall { val id: Int = 28; val name: String = "Truck w/Flatbed" }
  case object CraneTruck extends Label with CatSmall { val id: Int = 32; val name: String = "Crane Truck" }
  case object Motorboat extends Label with CatSmall { val id: Int = 41; val name: String = "Motorboat" }
  case object DumpTruck extends Label with CatSmall { val id: Int = 60; val name: String = "Dump Truck" }
  case object FrontLoaderOrBulldozer extends Label with CatSmall {
    val id: Int = 63; val name: String = "Front loader/Bulldozer"
  }
  case object ScraperOrTractor extends Label with CatSmall { val id: Int = 62; val name: String = "Scraper/Tractor" }
  case object Excavator extends Label with CatSmall { val id: Int = 64; val name: String = "Excavator" }
  case object CementMixer extends Label with CatSmall { val id: Int = 65; val name: String = "Cement Mixer" }
  case object GroundGrader extends Label with CatSmall { val id: Int = 66; val name: String = "Ground Grader" }
  case object ShippingContainer extends Label with CatSmall {
    val id: Int = 91; val name: String = "Shipping Container"
  }

  // Medium

  case object FixedWingAircraft extends Label with CatMedium {
    val id: Int = 11; val name: String = "Fixed-wing Aircraft"
  }
  case object SmallAircraft extends Label with CatMedium { val id: Int = 12; val name: String = "Small Aircraft" }
  case object Helicopter extends Label with CatMedium { val id: Int = 15; val name: String = "Helicopter" }
  case object TruckWithBox extends Label with CatMedium { val id: Int = 25; val name: String = "Truck w/Box" }
  case object TruckWithLiquid extends Label with CatMedium { val id: Int = 29; val name: String = "Truck w/Liquid" }
  case object RailwayVehicle extends Label with CatMedium { val id: Int = 33; val name: String = "Railway Vehicle" }
  case object PassengerCar extends Label with CatMedium { val id: Int = 34; val name: String = "Passenger Car" }
  case object CargoCar extends Label with CatMedium { val id: Int = 35; val name: String = "Cargo Car" }
  case object FlatCar extends Label with CatMedium { val id: Int = 36; val name: String = "Flat Car" }
  case object TankCar extends Label with CatMedium { val id: Int = 37; val name: String = "Tank car" }
  case object Locomotive extends Label with CatMedium { val id: Int = 38; val name: String = "Locomotive" }
  case object Sailboat extends Label with CatMedium { val id: Int = 42; val name: String = "Sailboat" }
  case object Tugboat extends Label with CatMedium { val id: Int = 44; val name: String = "Tugboat" }
  case object FishingVessel extends Label with CatMedium { val id: Int = 47; val name: String = "Fishing Vessel" }
  case object Yacht extends Label with CatMedium { val id: Int = 50; val name: String = "Yacht" }
  case object EngineeringVehicle extends Label with CatMedium {
    val id: Int = 53; val name: String = "Engineering Vehicle"
  }
  case object ReachStacker extends Label with CatMedium { val id: Int = 56; val name: String = "Reach Stacker" }
  case object MobileCrane extends Label with CatMedium { val id: Int = 59; val name: String = "Mobile Crane" }
  case object HaulTruck extends Label with CatMedium { val id: Int = 61; val name: String = "Haul Truck" }
  case object HutOrTent extends Label with CatMedium { val id: Int = 71; val name: String = "Hut/Tent" }
  case object Shed extends Label with CatMedium { val id: Int = 72; val name: String = "Shed" }
  case object Building extends Label with CatMedium { val id: Int = 73; val name: String = "Building" }
  case object DamagedBuilding extends Label with CatMedium { val id: Int = 76; val name: String = "Damaged Building" }
  case object Helipad extends Label with CatMedium { val id: Int = 84; val name: String = "Helipad" }
  case object StorageTank extends Label with CatMedium { val id: Int = 86; val name: String = "Storage Tank" }
  case object Pylon extends Label with CatMedium { val id: Int = 93; val name: String = "Pylon" }
  case object Tower extends Label with CatMedium { val id: Int = 94; val name: String = "Tower" }

  // LARGE

  case object CargoPlane extends Label with CatLarge { val id: Int = 13; val name: String = "Cargo Plane" }
  case object MaritimeVessel extends Label with CatLarge { val id: Int = 40; val name: String = "Maritime Vessel" }
  case object Barge extends Label with CatLarge { val id: Int = 45; val name: String = "Barge" }
  case object Ferry extends Label with CatLarge { val id: Int = 49; val name: String = "Ferry" }
  case object ContainerShip extends Label with CatLarge { val id: Int = 51; val name: String = "Container Ship" }
  case object OilTanker extends Label with CatLarge { val id: Int = 52; val name: String = "Oil Tanker" }
  case object TowerCrane extends Label with CatLarge { val id: Int = 54; val name: String = "Tower crane" }
  case object ContainerCrane extends Label with CatLarge { val id: Int = 55; val name: String = "Container Crane" }
  case object StraddleCarrier extends Label with CatLarge { val id: Int = 57; val name: String = "Straddle Carrier" }
  case object AircraftHangar extends Label with CatLarge { val id: Int = 74; val name: String = "Aircraft Hangar" }
  case object Facility extends Label with CatLarge { val id: Int = 77; val name: String = "Facility" }
  case object ConstructionSite extends Label with CatLarge { val id: Int = 79; val name: String = "Construction Site" }
  case object VehicleLot extends Label with CatLarge { val id: Int = 83; val name: String = "Vehicle Lot" }
  case object ShippingContainerlot extends Label with CatLarge {
    val id: Int = 89; val name: String = "Shipping container lot"
  }

  //

  sealed trait Split
  case object Small extends Split
  case object Medium extends Split
  case object Large extends Split

  sealed trait Cat {
    def split: Split
  }
  trait CatSmall {
    val split: Split = Small
  }
  trait CatMedium {
    val split: Split = Medium
  }
  trait CatLarge {
    val split: Split = Large
  }
}
