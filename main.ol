type Path             : string( regex( "([A-Za-z_][A-Za-z_0-9]*\\.)*([A-Za-z_][A-Za-z_0-9]*)" ) )

// MATCH

type Match            : void {
  data*               : undefined
  query               : Match_Exp
}

type Match_Exp        : bool | EQ_Exp | EXISTS_Exp | OR_Exp | AND_Exp | NOT_Exp

type EQ_Exp           : void { equal: EQ_Data | EQ_Path }

type EQ_Data          : void { 
  path                : Path
  data*               : undefined
}

type EQ_Path          : void {
  left                : Path
  right               : Path
}

type EXISTS_Exp       : void {
  exists              : Path
}

type OR_Exp           : void {
  or                  : Binary_Exp
}

type AND_Exp          : void {
  and                 : Binary_Exp
}

type NOT_Exp          : void {
  not                 : Match_Exp
}

type Binary_Exp       : void {
  left                : Match_Exp
  right               : Match_Exp
}

// UNWIND

type Unwind           : void {
  data*               : undefined
  query               : Path
}

// PROJECT

type Project          : void {
  data*               : undefined
  query[1,*]          : Project_Exp
}

type Project_Exp      : Path | ValuesToPath_Exp

type ValuesToPath_Exp : void {
  dstPath             : Path
  value[1,*]          : Value
}

type Value            : any | ValuePath | ValueMatch | ValueTernary

type ValuePath        : void {
  path                : Path
}

type ValueMatch       : void {
  match               : Match_Exp
}

type ValueTernary     : void {
	condition         : Match_Exp
	ifTrue[1,*]       : Value
	ifFalse[1,*]      : Value
}

// GROUP

type Group             : void {
  data*                : undefined
  query                : Group_Exp
}

type Group_Exp         : void {
  aggregate*           : Aggregate
  groupBy*             : GroupBy
}

type GroupBy           : void {
  dstPath              : Path
  srcPath              : Path
}

type Aggregate         : void {
  dstPath              : Path
  srcPath              : Path
  distinct?            : bool  //<< default is false
}

// LOOKUP

type Lookup            : void {
  leftData*            : undefined
  leftPath             : Path
  rightData*           : undefined
  rightPath            : Path
  dstPath              : Path
}

// PIPELINE

type Pipeline                   : void {
  data*                         : undefined
  pipeline[1,*]                 :
      void { matchQuery         : Match_Exp }
    | void { projectQuery[1,*]  : Project_Exp }
    | void { unwindQuery        : Path  }
    | void { groupQuery         : Group_Exp }
    | void { lookupQuery        : void {
        leftPath   : Path
        rightData* : undefined
        rightPath  : Path
        dstPath    : Path
      }
    }
}

// QUERY RESPONSE

type QueryResponse     : void {
  result*              : undefined
  queryTime?           : long
}


interface TQueryInterface {
  RequestResponse :
  match    ( Match     )( QueryResponse ) throws MalformedQuery_Expression( string ),
  unwind   ( Unwind    )( QueryResponse ) throws MalformedQuery_Expression( string ),
  project  ( Project   )( QueryResponse ) throws MalformedQuery_Expression( string ) MergeValueException( string ),
  group    ( Group     )( QueryResponse ) throws MalformedQuery_Expression( string ),
  lookup   ( Lookup    )( QueryResponse ) throws MalformedQuery_Expression( string ),
  pipeline ( Pipeline  )( QueryResponse ) throws MalformedQuery_Expression( string )
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
