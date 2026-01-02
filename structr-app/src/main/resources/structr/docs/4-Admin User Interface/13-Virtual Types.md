# Virtual Types

Virtual Types provide a powerful abstraction layer for data transformation and integration in Structr. Unlike regular schema types that persist data directly to the database, Virtual Types act as dynamic data processors that can transform, aggregate, or reshape data on-the-fly.

![Virtual Types](../virtual-types.png)

## Overview

Virtual Types are non-persisting data types that behave like regular schema types in many ways—they can be queried via REST API, used in pages, and referenced in scripts—but with a fundamental difference: their data is not stored as database objects. Instead, Virtual Types use transformation functions embedded in virtual properties to dynamically generate their output from other data sources.

This makes Virtual Types ideal for scenarios such as creating API facades over existing data structures, aggregating data from multiple sources into a unified view, transforming data formats for external system integration, and building computed views without data duplication.

## Key Concepts

Virtual Types consist of virtual properties, each containing transformation logic that defines how data is sourced and processed. When a Virtual Type is queried, these transformation functions execute to produce the response dynamically.

The Virtual Types interface allows you to create and configure virtual types, define virtual properties with custom transformation functions, test and debug your transformations, and manage the lifecycle of your virtual type definitions.