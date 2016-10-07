<img src="http://static.nfl.com/static/content/public/static/img/logos/nfl-engineering-light.svg" width="300" />

# GLiTR: GraphQL Lightweight Type Registry

[![][travis img]][travis]
[![][maven img]][maven]
[![][license img]][license]

A library that let you use Plain Old Java Objects to describe your GraphQL schema.

## Binaries

Example for Maven:

```xml
<dependency>
    <groupId>com.nfl.glitr</groupId>
    <artifactId>glitr</artifactId>
    <version>x.y.z</version>
</dependency>
```

Example for gradle:

```gradle
compile("com.nfl.glitr:glitr:x.y.z")
```

Change history can be found here: [CHANGELOG.md](https://github.com/NFL/glitr/blob/master/CHANGELOG.md)

## How to use it

This is the famous "hello world" in [graphql-java](https://github.com/graphql-java/graphql-java) with GLiTR:

```java
import com.nfl.glitr.Glitr;
import com.nfl.glitr.GlitrBuilder;
import com.nfl.glitr.annotation.GlitrDescription;
import graphql.GraphQL;
import graphql.schema.DataFetchingEnvironment;

import java.util.Map;

public class HelloWorld {

    public static void main(String[] args) {

        Glitr glitr = GlitrBuilder.newGlitr()
                .withQueryRoot(new Root())
                .build();

        GraphQL graphQL = new GraphQL(glitr.getSchema());

        Map<String, Object> result = (Map<String, Object>) graphQL.execute("{hello}").getData();

        System.out.println(result);
        // Prints: {hello=World!}
    }

    @GlitrDescription("Where it all begins.")
    public static class Root {

        public String getHello(DataFetchingEnvironment environment) {
            return "World!";
        }
    }
}
```

## Full Documentation

See the [Wiki](https://github.com/NFL/glitr/wiki/) for full documentation, examples, operational details and other information.

## Build

To build:

```
$ git clone git@github.com:NFL/glitr.git
$ cd glitr/
$ ./gradlew build
```

Further details on building can be found on the [Getting Started](https://github.com/NFL/glitr/wiki/Getting-Started) page of the wiki.

## Requirements

 - >= Java 8

## Examples 

See [glitr-examples](https://github.com/NFL/glitr/tree/master/glitr-examples/src/main/java/com/nfl/dm/glitr/examples) for example implementation

## Contact Info

- Twitter: [@nflengineers](http://twitter.com/nflengineers)
- [GitHub Issues](https://github.com/NFL/glitr/issues)


## LICENSE

GLiTR is license under the MIT License. See [LICENSE](LICENSE) for more details.