package com.github.jw3.xview

object Labels {
  sealed trait Label {
    def id: Int
    def name: String
  }

  case object FixedWingAircraft extends Label { val id: Int = 11; val name: String = "Fixed-wing Aircraft" }
  case object SmallAircraft extends Label { val id: Int = 12; val name: String = "Small Aircraft" }
  case object CargoPlane extends Label { val id: Int = 13; val name: String = "Cargo Plane" }
  case object Helicopter extends Label { val id: Int = 15; val name: String = "Helicopter" }
  case object PassengerVehicle extends Label { val id: Int = 17; val name: String = "Passenger Vehicle" }
  case object SmallCar extends Label { val id: Int = 18; val name: String = "Small Car" }
  case object Bus extends Label { val id: Int = 19; val name: String = "Bus" }
  case object PickupTruck extends Label { val id: Int = 20; val name: String = "Pickup Truck" }
  case object UtilityTruck extends Label { val id: Int = 21; val name: String = "Utility Truck" }
  case object Truck extends Label { val id: Int = 23; val name: String = "Truck" }
  case object CargoTruck extends Label { val id: Int = 24; val name: String = "Cargo Truck" }
  case object TruckWithBox extends Label { val id: Int = 25; val name: String = "Truck w/Box" }
  case object TruckTractor extends Label { val id: Int = 26; val name: String = "Truck Tractor" }
  case object Trailer extends Label { val id: Int = 27; val name: String = "Trailer" }
  case object TruckWithFlatbed extends Label { val id: Int = 28; val name: String = "Truck w/Flatbed" }
  case object TruckWithLiquid extends Label { val id: Int = 29; val name: String = "Truck w/Liquid" }
  case object CraneTruck extends Label { val id: Int = 32; val name: String = "Crane Truck" }
  case object RailwayVehicle extends Label { val id: Int = 33; val name: String = "Railway Vehicle" }
  case object PassengerCar extends Label { val id: Int = 34; val name: String = "Passenger Car" }
  case object CargoCar extends Label { val id: Int = 35; val name: String = "Cargo Car" }
  case object FlatCar extends Label { val id: Int = 36; val name: String = "Flat Car" }
  case object TankCar extends Label { val id: Int = 37; val name: String = "Tank car" }
  case object Locomotive extends Label { val id: Int = 38; val name: String = "Locomotive" }
  case object MaritimeVessel extends Label { val id: Int = 40; val name: String = "Maritime Vessel" }
  case object Motorboat extends Label { val id: Int = 41; val name: String = "Motorboat" }
  case object Sailboat extends Label { val id: Int = 42; val name: String = "Sailboat" }
  case object Tugboat extends Label { val id: Int = 44; val name: String = "Tugboat" }
  case object Barge extends Label { val id: Int = 45; val name: String = "Barge" }
  case object FishingVessel extends Label { val id: Int = 47; val name: String = "Fishing Vessel" }
  case object Ferry extends Label { val id: Int = 49; val name: String = "Ferry" }
  case object Yacht extends Label { val id: Int = 50; val name: String = "Yacht" }
  case object ContainerShip extends Label { val id: Int = 51; val name: String = "Container Ship" }
  case object OilTanker extends Label { val id: Int = 52; val name: String = "Oil Tanker" }
  case object EngineeringVehicle extends Label { val id: Int = 53; val name: String = "Engineering Vehicle" }
  case object TowerCrane extends Label { val id: Int = 54; val name: String = "Tower crane" }
  case object ContainerCrane extends Label { val id: Int = 55; val name: String = "Container Crane" }
  case object ReachStacker extends Label { val id: Int = 56; val name: String = "Reach Stacker" }
  case object StraddleCarrier extends Label { val id: Int = 57; val name: String = "Straddle Carrier" }
  case object MobileCrane extends Label { val id: Int = 59; val name: String = "Mobile Crane" }
  case object DumpTruck extends Label { val id: Int = 60; val name: String = "Dump Truck" }
  case object HaulTruck extends Label { val id: Int = 61; val name: String = "Haul Truck" }
  case object ScraperOrTractor extends Label { val id: Int = 62; val name: String = "Scraper/Tractor" }
  case object FrontLoaderOrBulldozer extends Label { val id: Int = 63; val name: String = "Front loader/Bulldozer" }
  case object Excavator extends Label { val id: Int = 64; val name: String = "Excavator" }
  case object CementMixer extends Label { val id: Int = 65; val name: String = "Cement Mixer" }
  case object GroundGrader extends Label { val id: Int = 66; val name: String = "Ground Grader" }
  case object HutOrTent extends Label { val id: Int = 71; val name: String = "Hut/Tent" }
  case object Shed extends Label { val id: Int = 72; val name: String = "Shed" }
  case object Building extends Label { val id: Int = 73; val name: String = "Building" }
  case object AircraftHangar extends Label { val id: Int = 74; val name: String = "Aircraft Hangar" }
  case object DamagedBuilding extends Label { val id: Int = 76; val name: String = "Damaged Building" }
  case object Facility extends Label { val id: Int = 77; val name: String = "Facility" }
  case object ConstructionSite extends Label { val id: Int = 79; val name: String = "Construction Site" }
  case object VehicleLot extends Label { val id: Int = 83; val name: String = "Vehicle Lot" }
  case object Helipad extends Label { val id: Int = 84; val name: String = "Helipad" }
  case object StorageTank extends Label { val id: Int = 86; val name: String = "Storage Tank" }
  case object ShippingContainerlot extends Label { val id: Int = 89; val name: String = "Shipping container lot" }
  case object ShippingContainer extends Label { val id: Int = 91; val name: String = "Shipping Container" }
  case object Pylon extends Label { val id: Int = 93; val name: String = "Pylon" }
  case object Tower extends Label { val id: Int = 94; val name: String = "Tower" }
}
