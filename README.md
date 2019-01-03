# OpenJAX Standard JSON

**Java API Extension for JSON Parsing**

## Introduction

The OpenJAX Standard JSON library provides `JsonReader` and `JsonParser`.

## JsonReader

The `JsonReader` is a subclass of `Reader` that reads JSON documents and validates the content conforms to the [RFC 4627][4627] specification. In addition to the standard "read" methods defined in `Reader`, the `JsonReader` provides the following patterns:

1. Standard reading of ["JSON tokens"](#json-tokens):
    1. Via `JsonReader#readToken()`, which returns tokens as `String`s.
2. Iterator reading of ["JSON tokens"](#json-tokens). The `JsonReader` implements the `Iterable` interface:
    1. Via `Iterator#next()`, which returns tokens as `String`s.
3. Optimized reading of ["JSON tokens"](#json-tokens):
    1. `JsonReader#readTokenStart()`, which returns the start index of the token.
4. Partial reading of ["JSON tokens"](#json-tokens):
    1. Via: `Reader#read()`.
    2. Via: `Reader#read(char[])`.
    3. Via: `Reader#read(char[], int, int)`.
5. The `JsonReader` is a `ReplayReader`, which allows a JSON document to be re-read:
    1. Via: `JsonReader#setPosition(int)`: The character position to be reset to previous point in the stream being read.
    2. Via: `JsonReader#setIndex(int)`: The token index position to be reset to a previous point of enumerated ["JSON tokens"](#json-tokens).
6. Ignore or preserve inter-token whitespace.
7. Unescapes string-literal JSON escape codes as defined in [RFC 4627, Section 2.5][4627].

### JsonParser

The `JsonParser` is a validating parser for JSON documents that asserts content conforms to the [RFC 4627][4627] specification. The `JsonParser` is designed with performance in mind, and utilizes the `JsonReader#readTokenStart()` method to avoid instantiation of `String` object. The `JsonParser` accepts a `JsonHandler` that defines methods for the reconstitution of ["JSON tokens"](#json-tokens) from the provided _start and _end_ token indices.

### JSON Tokens

| JSON Tokens | |
|-|-|
| **Structural** | |
| &nbsp;&nbsp;A character that is one of: | `[{}\[\]:,]` |
| **A property key** | |
| &nbsp;&nbsp;A string that matches: | `^".*"$` |
| **A property or array member value** | |
| &nbsp;&nbsp;A string that matches: | `^".*"$` |
| &nbsp;&nbsp;A number that matches: | `^-?(([0-9])\|([1-9][0-9]+))(\.[\.0-9]+)?`<br>`([eE][+-]?(([0-9])\|([1-9][0-9]+)))?$` |
| &nbsp;&nbsp;A literal that matches: | `^(null)\|(true)\|(false)$` |
| **Whitespace** | |
| &nbsp;&nbsp;Whitespace string that matches: | `^[ \n\r\t]+$` |

### JavaDocs

JavaDocs are available [here](https://standard.openjax.org/json/apidocs/).

## Contributing

Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.

Please make sure to update tests as appropriate.

### License

This project is licensed under the MIT License - see the [LICENSE.txt](LICENSE.txt) file for details.

[4627]: https://www.ietf.org/rfc/rfc4627.txt