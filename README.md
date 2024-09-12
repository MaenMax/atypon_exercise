# Atypon Exercise - Recipe API

**Author**: Maen Abu Hammour  
**Date**: September 11, 2024

## Project Overview

This Spring Boot project provides a set of REST APIs that interact with the Spoonacular API to fetch recipe information, including details such as ingredients and calorie count. Additionally, the project offers functionality to customize recipe calorie calculations by excluding specific ingredients.

## Table of Contents

- [Features](#features)
- [Project Structure](#project-structure)
- [Endpoints](#endpoints)
- [How to Run](#how-to-run)
- [Makefile Commands](#makefile-commands)
- [Technologies Used](#technologies-used)
- [License](#license)

## Features

- **Search for recipes**: Search for recipes using the Spoonacular API by specifying a recipe name and number of results to retrieve.
- **Get recipe information**: Fetch detailed information about a specific recipe, including its ingredients and total calories.
- **Customize recipe information**: Exclude certain ingredients from the calorie calculation of a recipe and return the updated total calories.

## Project Structure

```bash
atypon-exercise/
│
├── src/main/java/com/atypon/exercise
│   └── RecipeController.java    # Main controller for handling recipe APIs
│   └── SpringBootApp.java       # Spring Boot application entry point
│
│── src/test/java/com/atypon/excercise
│   └── AppTest.java             # Test file
│
├── target/                      # Compiled code (after running build)
├── pom.xml                      # Maven build configuration
├── Makefile                     # Makefile for project build, clean, and run commands
└── README.md                    # Project documentation
```
## Endpoints

1. **Search for Recipes**
   - **Endpoint**: `/recipes/search`
   - **Method**: `GET`
   - **Parameters**:
     - `name` (String): Name of the recipe to search for.
     - `number` (int): Number of results to return.
   - **Example Request**:
     GET http://localhost:8080/recipes/search?name=pasta&number=1
     ```
   - **Example Response**:
     ```json
     {
       "results": [
         {
           "id": 642583,
           "title": "Farfalle with Peas, Ham and Cream",
           "image": "https://img.spoonacular.com/recipes/642583-312x231.jpg",
           "imageType": "jpg"
         }
       ],
       "offset": 0,
       "number": 1,
       "totalResults": 285
     }
     ```

2. **Get Recipe Information**
   - **Endpoint**: `/recipes/{name}/information`
   - **Method**: `GET`
   - **Parameters**:
     - `name` (String): The name of the recipe.
   - **Example Request**:
     ```bash
     GET http://localhost:8080/recipes/pasta/information
     ```
   - **Example Response**:
     ```bash
     1590.56
     ```

3. **Get Customized Recipe Information**
   - **Endpoint**: `/recipes/{name}/information`
   - **Method**: `POST`
   - **Request Body**:
     - `ingredientsToExclude` (Array of Strings): List of ingredients to exclude from the recipe calorie calculation.
   - **Example Request**:
     ```bash
     POST http://localhost:8080/recipes/curry/information
     ```
     ```json
     {
         "ingredientsToExclude": ["jaggery", "chili"]
     }
    ```
   - **Example Response**:
     ```bash
     553.76
     ```

## How to Run

### Prerequisites
- **Java 11** or higher installed.
- **Maven** installed.

### Steps to Run

1. **Clone the project**:
   ```bash
   git clone git@github.com:MaenMax/atypon_exercise.git
   cd atypon_exercise
   ```
1. **build and run the project**:
   ```bash
   mvn spring-boot:run
   ```
   the aplication is going to start on port 8080