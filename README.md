
# WeatherApp

**WeatherApp 1.0 Release Notes 18/01/21** 

 - Initial WeatherApp release 

**Important!**

To build the app you will need to add an openweathermap.org API key to your gradle.properties file which is outside of the project (e.g. /Users/[user_dir]/.gradle/gradle.properties) on mac. Add a line to the gradle.properties as follows:

weather_api_key = "[your openweathermap api key]"

**Assumptions**

 - Main weather only - screenshot doesn’t show description.
 - UI - Colours, fonts sizes, margins/padding etc.
 - Layout behaviours - e.g. how does current condition icon behave with long current condition text (wrap/truncate etc).
 - 8 Point Compass - based on screen shot example.
 - Round temp and wind speed to nearest whole number.
 - Age of data is based on timestamp from api call not actual time of api call.
 - If data can’t be synced for any reason use latest as per (<24) rules (not just for no internet) and show general error.


## **Technical Decisions**

**Architecture:**

MVVM/MVI Architecture 
  - Enough to demonstrate a production quality app focusing on stability, extendability and testability.

Libs & tech:

 - DI -> Koin 
 - DB -> Realm 
 - Coroutines 
 - LiveData 
 - Navigation Framework
 - Retrofit     
 - Glide     
 - LocationManager     
 - Dexter permissions library

## **Trade-Offs**

  - No feature files as only one feature; in a real production app business logic would be moved into features
  - No separate repository data sources; in a real production app repository would be setup with separate data sources
  - Only unit tested extension functions & happy paths for both ViewModel & Repository but this covers most business logic


## **Improvements**

 - Unhappy paths for GPS, location services more elaborate
 - More testing; better unit test coverage as well as android/UI tests

  
