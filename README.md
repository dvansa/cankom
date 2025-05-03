# CanKom <img src="app/src/main/res/mipmap-hdpi/ic_cankom_round.webp" width="42">
![CI Badge](https://github.com/dvansa/cankom/actions/workflows/app.yml/badge.svg) ![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)

*Can I commute?*
An Android utility app that tells if your next daily commute is affected by weather.

## Usage
(Only usable in Switzerland and border areas) 

Configure your daily commute route, leave/arrival times and a suitable temperature range.

<img src="assets/settings.jpg" width=250/> <img src="assets/commute_route.gif" width=250/>

Checks if your next commute will be affected by:
- Hot/Cold temperatures
- Precipitations

<img src="assets/commute_status_1.jpg" width=250/> <img src="assets/commute_status_3.jpg" width=250/> <img src="assets/commute_status_2.jpg" width=250/>

Precipitation checks are accurately carried out intersecting your route with weather forecast radar data maps.

<img src="assets/map_precipitation_1.jpg" width=250/> <img src="assets/map_precipitation_2.jpg" width=250/>

## Setup
### Create Maps API Key

1. Follow instructions to generate a [Google Maps API key](https://developers.google.com/maps/documentation/embed/get-api-key#create-api-keys)
2. Create a `local.properties` file and write the following line with your API key:
```
API_KEY=<google maps API key>
```
### Build App
1. Download, install and setup [Android Studio](https://developer.android.com/sdk/index.html)
2. Clone this repository and open its root folder as project in Android Studio
3. Click on build button

## Legal

- Unless otherwise explicited in file comments, the whole project is under [MIT License](LICENSE).

- Http requests are sent to https://www.meteoswiss.admin.ch to retrieve meteorological forecast information. 
Users must abide to the legal basis of Meteo Swiss before attempting any use of the app or any derivative works. In no event shall the authors of the repository be liable for any potential infringiments caused by users to Meteo Swiss services.