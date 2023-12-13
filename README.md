<img src="https://github.com/jolie/tquery/raw/master/tquery_logo.png" width="100">

The <a href="https://github.com/jolie/tquery">Tquery</a> project is a query framework integrated in the Jolie language for the data handling/querying of Jolie trees.

Tquery is based on a [tree-based formalization](https://arxiv.org/abs/1904.11327) (language and semantics) of [MQuery](https://arxiv.org/abs/1603.09291), a sound variant of the Aggregation Framework, the query language of the most popular document-oriented database: MongoDB.

Tree-shaped documents are the main format in which data flows within modern digital systems --- e.g., eHealth, the Internet-of-Things, and Edge Computing.

Tquery is particularly suited to develop real-time, ephemeral scenarios, where data shall not persist in the system.

If you use this software, please cite it using the following bibitem.

```bibtex
@article{GMSZ22,
  author    = {Saverio Giallorenzo and
              Fabrizio Montesi and
              Larisa Safina and
              Stefano Pio Zingaro},
  title     = {Ephemeral data handling in microservices with Tquery},
  journal   = {PeerJ Comput. Sci.},
  volume    = {8},
  pages     = {e1037},
  year      = {2022},
  doi       = {10.7717/peerj-cs.1037},
}
```

# Installation

`jpm add @jolie/tquery`

# Import

```jolie
from @jolie.tquery.main import TQuery
```

# Operators

TQuery currently include the following operators: [Match](#Match), [Unwind](#Unwind), [Project](#Project), [Group](#Group), [Join](#Join), and [Pipeline](#Pipeline). 

To illustrate the operators currently implemented in TQuery, we use the following data structure

```js
myData = [{"y":[2020], 
  "M":[{"m":[11], 
      "D":[{"d":[27], "L":[{s:['23:33'], "e":['07:04'], "q":['poor']}]},
          {"d":[28], "L":[{"s":['21:13'], "e":['09:34'], "q":['good']}]},
          {"d":[29], "L":[{"s":['21:01'], "e":['03:12'], "q":['good']},
                      {"s":['03:36'], "e":['09:58'], "q":['good']}]},
          {"d":[30], "L":[{"s":['20:33'], "e":['01:14'], "q":['poor']},
                      {"s":['01:32'], "e":['06:15'], "q":['good']}]}
      ]}]}]
}]
```
## Match

The purpose of the match operator is to select trees in a given array according to a criterion, which can be: i) the Boolean truth, ii) the existence of a path, iii) the equality between the array pointed by a path and a given array, iv) the equality between the arrays reached via two paths `p1` and `p2` on the given array, and the logic connectives v) negation, vi) conjunction, and vii) disjunction over the previous criteria.

For example, the invocation below selects (in `resp`) all elements whose `date` corresponds to either `2020` `11` `29` or `2020` `11` `30`.

```jolie
match@Tquery( { 
 data << myData
 query.and << { 
  left.equal    << { path = "y" data = 2020 }
  right.and << {
   left.equal   << { path = "M.m" data = 11 }
   right.or << {
    left.equal  << { path = "M.D.d" data = 29 }
    right.equal << { path = "M.D.d" data = 30 }
}}}})( resp )
```

## Unwind

The unwind operator unfolds the elements of an array under a given path.

For example, the invocation

```jolie
unwind@Tquery( { data << myData query = "M.D.L" } )( unwindResp )
```

unfolds the data structure along the path `M.D.L`, returning

```js
[ { "y":[ 2020 ], "M":[ {"m":[ 11 ],"D":[ { "d":[ 27 ],
    "L":[ { "s":[ '23:33' ],"e":[ '07:04' ],"q":[ 'poor' ] }] }] }] },
  { "y":[ 2020 ],"M":[ {"m":[ 11 ],"D":[ {"d":[ 28 ],
    "L":[ { "s":[ '21:13' ],"e":[ '09:34' ],"q":[ 'good' ] }] }] }] },
  { "y":[ 2020 ],"M":[ {"m":[ 11 ],"D":[ {"d":[ 29 ],
    "L":[ { "s":[ '21:01'{} ],"e":[ '03:12'{} ],"q":[ 'good'{} ] }] }] }] },
  //...
]
```

## Project

The project operator modifies the trees in an array by projecting nodes, renaming node labels, or introducing new nodes as value definitions. A value definition is either i) a value, ii) a path, iii) an array of value definitions, iv) a selection criterion or v) a ternary expression on a criterion and two value definitions.

For example, the invocation

```jolie
 project@Tquery( { 
  data << unwindResp.result
  query[0] << {dstPath = "year"    value.path = "y"       }
  query[1] << {dstPath = "month"   value.path = "M.m"     }
  query[2] << {dstPath = "day"     value.path = "M.D.d"   }
  query[3] << {dstPath = "quality" value.path = "M.D.L.q" }
 })( projResp )
```

moves and renames into their respective `dstPath`s the node `y` into `year`, the node `M.m` into `month` (same level as `year`), etc. This results into the data structure

```js
[ { "year":[ 2020 ], "month":[ 11 ],"day":[ 27 ], "quality":[ 'poor' ] },
  { "year":[ 2020 ], "month":[ 11 ],"day":[ 28 ], "quality":[ 'good' ] },
  { "year":[ 2020 ], "month":[ 11 ],"day":[ 29 ], "quality":[ 'good' ] },
  //...
]
```

## Group

This operator aggregates the trees in an array according to an aggregation specification and it groups the values of the aggregated trees according to a grouping specification.

For example, the following invocation aggregates the elements in the projected data structure, above, by `quality`. There, the aggregation specification indicates to project the `quality` node into a node with the same name

```jolie
group@Tquery( {
data << projResp.result
query.aggregate <<{ dstPath="quality" srcPath="quality"}
})( groupResp )
```

obtaining a data structure like

```json
[ { "year":[2020],"month":[11],"day":[29],"quality":["good", "good"]} ,
  { "year":[2020],"month":[11],"day":[30],"quality":["poor", "good"]} ]
```

## Lookup

The lookup operator joins trees in a source array with the trees in an adjunct array.
The user indicates two paths, one for each array, so that the lookup operators can join trees in the two arrays whose values pointed by their respective paths coincide. If there is a match, the operator projects the matching value of the adjunct array into into the value of the source array, under a given projection path.

For example, considering the data structures
```js
cities = [ { "city" : "Bologna", "temp" : 23, country: "IT" }, 
         { "city" : "Odense", "temp": 13, country: "DK" }, 
         { "city" : "Imola", "temp" : 22, country: "IT" } ]
```
and
```js
nations = [ { "cid" : "IT", "affiliation" : "EU" }, 
         { "cid" : "DK", "affiliation": "EU" } ]
```

invoking the operation

```jolie
lookup@Tquery( {
leftData  << cities  leftPath = "country"
rightData << nations  rightPath = "cid"
dstPath = "aff"
})( resp )
```

would return a data structure of the shape

```js
[ { "city" : "Bologna", "temp" : 23, country: "IT", "aff" : { "cid": "IT", "affiliation" : "EU" } }, 
  { "city" : "Odense", "temp": 13, country: "DK", "aff" : { "cid": "DK", "affiliation" : "EU" } }, 
  { "city" : "Imola", "temp" : 22, country: "IT",  "aff" : { "cid": "IT", "affiliation" : "EU" } } ]
```

# Pipeline

TQuery also accepts the definition of multi-stage queries (e.g., to increase performance).

The pipeline operation preserves almost the same syntax seen for each of the operators above, with the main difference that the user specifies a sequence (as an array) of queries.

For example, this invocation yeilds the same result as calling in sequence the examples shown for the unwind and project operators

```jolie
ps[0].unwindQuery = "M.D.L"

ps[1] << { projectQuery[0] << 
  { dstPath = "year" value.path = "y" }
 projectQuery[1] << 
  { dstPath = "month" value.path = "M.m" }
 projectQuery[2] << 
  { dstPath = "day" value.path = "M.D.d" }
 projectQuery[3] << 
  { dstPath = "quality" value.path = "M.D.L.q" }
}

pipeline@Tquery({ data << myData pipeline << ps })( resp )
```
