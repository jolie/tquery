/*****************************************************************************
 *  Copyright (C) 2018 by Larisa Safina <safina@imada.sdu.dk>                *
 *  Copyright (C) 2018 by Stefano Pio Zingaro <stefanopio.zingaro@unibo.it>  *
 *  Copyright (C) 2019 by Saverio Giallorenzo <saverio.giallorenzo@gmail.com>*
 *                                                                           *
 *  This program is free software; you can redistribute it and/or modify     *
 *  it under the terms of the GNU Library General Public License as          *
 *  published by the Free Software Foundation; either version 2 of the       *
 *  License, or (at your option) any later version.                          *
 *                                                                           *
 *  This program is distributed in the hope that it will be useful,          *
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of           *
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the            *
 *  GNU General Public License for more details.                             *
 *                                                                           *
 *  You should have received a copy of the GNU Library General Public        *
 *  License along with this program; if not, write to the                    *
 *  Free Software Foundation, Inc.,                                          *
 *  59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.                *
 *                                                                           *
 *  For details about the authors of this software, see the AUTHORS file.    *
 *****************************************************************************/

from console import Console
from file import File
from string_utils import StringUtils
from "./packages/tquery/tquery" import TQuery

include "private/match.ol"


service main()
{ 
  embed File as File
  embed Console as Console
  embed StringUtils as StringUtils

  embed TQuery as TQuery

  main {
    getServiceDirectory@File()( serviceDirectory );
    getFileSeparator@File()( sep ); 
    datafilesDirectory = serviceDirectory + sep + "private";

    list@File( {
      .regex = ".*\\.json",
      .directory = datafilesDirectory
    } )( listResponse );

    for ( file in listResponse.result ) {
      readFile@File( {
        .filename = datafilesDirectory + sep + file,
        .format = "json"
      } )( readFileResponse );
      bios[ readFileResponse._id ] << readFileResponse
    };



    scope( doMatch )
    {
     install(
       IllegalArgumentException =>
        valueToPrettyString@StringUtils( doMatch )( t );
        println@Console( "IllegalArgumentException: " + t )()
       );

      println@Console( "TEST: exist, and, or. Expected result: bios[4]" )() ;
      match;
      match@TQuery( matchRequest )( matchResponse );
      valueToPrettyString@StringUtils( matchResponse )( t );
      println@Console( t )();
      undef( matchRequest );

      println@Console( "TEST: query = false. Expected result: empty value" )() ;
      match_bool;
      match@TQuery( matchRequest )( matchResponse );
      valueToPrettyString@StringUtils( matchResponse )( t );
      println@Console( t )();
      undef( matchRequest );

      println@Console( "TEST: query.not = true. Expected result: empty value" )() ;
      match_bool_bis;
      match@TQuery( matchRequest )( matchResponse );
      valueToPrettyString@StringUtils( matchResponse )( t );
      println@Console( t )();
      undef( matchRequest );

      println@Console( "TEST: query = true. Expected result: whole set" )() ;
      match_bool_opp;
      match@TQuery( matchRequest )( matchResponse );
      valueToPrettyString@StringUtils( matchResponse )( t );
      println@Console( t )();
      undef( matchRequest );

      println@Console( "TEST: structural equality with array of trees. Expected result: whole set" )() ;
      match_equal_value;
      match@TQuery( matchRequest )( matchResponse );
      valueToPrettyString@StringUtils( matchResponse )( t );
      println@Console( t )();
      undef( matchRequest );

      println@Console( "TEST: structural equality just with array of roots. Expected result: whole set" )() ;
      match_equal_value_bis;
      match@TQuery( matchRequest )( matchResponse )
      valueToPrettyString@StringUtils( matchResponse )( t );
      println@Console( t )();
      undef( matchRequest )
    }




      println@Console( "Hello, world!" )()
    }

}
