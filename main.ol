type Path               : string( regex( "[A-Za-z0-9._]*[A-Za-z0-9._]+" ) )

type MatchRequest       : void {
  data*                : undefined
  query                : MatchExp
}

type MatchExp           : UnaryExp | ORExp | ANDExp | NOTExp

type UnaryExp           : EQUALExp | EXISTSExp | bool

type EQUALExp           : void {
  equal                : void {
    path               : Path
    value[1,*]         : undefined
  }
}

type EXISTSExp          : void {
  exists               : Path
}

type ORExp              : void {
  or                   : BinaryExp
}

type ANDExp             : void {
  and                  : BinaryExp
}

type NOTExp             : void {
  not                  : MatchExp
}

type BinaryExp          : void {
  left                 : MatchExp
  right                : MatchExp
}

type UnwindRequest      : void {
  data*                : undefined
  query                : Path
}

type ProjectRequest     : void {
  data*                : undefined
  query[1,*]           : ProjectionExp
}

type ProjectionExp      : Path | ValuesToPathExp

type ValuesToPathExp    : void {
  dstPath              : Path
  value[1,*]           : Value
}

type Value              : any | ValuePath | ValueMatch | ValueTernary

type ValuePath          : void {
  path                 : Path
}

type ValueMatch         : void {
  match                : MatchExp
}

type ValueTernary         : void {
  ternary                : void {
		condition            : MatchExp
		ifTrue[1,*]          : Value
		ifFalse[1,*]         : Value
  }
}

type GroupRequest       : void {
  data*                : undefined
  query                : GroupExp
}

type GroupExp           : void {
  aggregate*           : AggregateDefinition
  groupBy*             : GroupDefinition
}

type GroupDefinition    : void {
  dstPath              : Path
  srcPath              : Path
}

type AggregateDefinition: void {
  dstPath              : Path
  srcPath              : Path
  distinct?            : bool  //<< default is false
}

type LookupRequest      : void {
  leftData*            : undefined
  leftPath             : Path
  rightData*           : undefined
  rightPath            : Path
  dstPath              : Path
}

type ResponseType       : void {
  result*              : undefined
}

type PipelineRequest    : void {
  data*                : undefined
  pipeline[1,*]        : 
    void {
      matchQuery       : MatchExp
    } 
    |
    void {
      projectQuery       : ProjectExp
    }
    |
    void {
      unwindQuery         : UnwindExp
    }
    |
    void {
      groupQuery          : GroupExp
    }
    |
    void {
      lookupQuery         : void {
        leftPath             : Path
        rightData*           : undefined
        rightPath            : Path
        dstPath              : Path
      }
    }
}

interface TQueryInterface {
  RequestResponse :
  match   ( MatchRequest  )( ResponseType ) throws MalformedQueryExpression( string ),
  unwind  ( UnwindRequest     )( ResponseType ) throws MalformedQueryExpression( string ),
  project ( ProjectRequest    )( ResponseType ) throws MalformedQueryExpression( string ) MergeValueException( string ),
  group   ( GroupRequest      )( ResponseType ) throws MalformedQueryExpression( string ),
  lookup  ( LookupRequest     )( ResponseType ) throws MalformedQueryExpression( string ),
  pipeline ( PipelineRequest )( ResponseType ) throws MalformedQueryExpression ( string )
}

service TQuery {
  inputPort IP {
    location: "local"
    interfaces: TQueryInterface
  }  

  foreign java {
    class: "joliex.tquery.engine.TQueryService"
  }
}
