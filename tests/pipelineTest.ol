/*****************************************************************************
 *  Copyright (C) 2021 by Larisa Safina <safina@imada.sdu.dk>                *
 *  Copyright (C) 2021 by Stefano Pio Zingaro <stefanopio.zingaro@unibo.it>  *
 *  Copyright (C) 2021 by Saverio Giallorenzo <saverio.giallorenzo@gmail.com>*
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
from json_utils import JsonUtils
from file import File
from string_utils import StringUtils
from @jolie.tquery.main import TQuery
from time import Time

service TQueryService 
{

  embed Console as Console
  embed JsonUtils as JsonUtils
  embed File as File
  embed StringUtils as StringUtils
  embed TQuery as TQuery
  embed Time as Time

  main {

      readFile@File( { .filename = "data/biometric-3.json" } )( biometric )
      readFile@File( { .filename = "data/sleeplog-3.json" } )( sleeplog )

      p -> biometricRequest.pipeline
      getJsonValue@JsonUtils( biometric )( bioDashedData )
      biometricRequest.data << bioDashedData._

      pseudoId = new

      with ( p[i].matchQuery.or ) {
        with(.left) {
          .equal.path = "date"
          .equal.data = 20201128
        }
        with (.right.or) {
          with (.right) {
            .equal.path = "date"
            .equal.data = 20201129
          }
          with (.left) {
            .equal.path = "date"
            .equal.data = 20201130
          }
        }
      }

      i++
      p[i].groupQuery.aggregate[0] << {
        .dstPath = "t"
        .srcPath = "t"
      }

      i++
      with( p[i] ){
        .projectQuery[0] << {
            dstPath = "t"
            value.path = "t"
        }
        .projectQuery[1] << {
            dstPath = "patient_id"
            value = pseudoId
        }
      }

      pipeline@TQuery( biometricRequest )( biometricResponse )


      i = 0
      p -> sleeplogRequest.pipeline
      getJsonValue@JsonUtils( sleeplog )( slDashedData )
      sleeplogRequest.data << slDashedData._
      
      p[i].unwindQuery = "M.D.L"
      
      i++
      with( p[i] ){
        .projectQuery[0] << {
          dstPath = "year"
          value.path = "y"
        }
        .projectQuery[1] << {
          dstPath = "month"
          value.path = "M.m"
        }
        .projectQuery[2] << {
          dstPath = "day"
          value.path = "M.D.d"
        }
        .projectQuery[3] << {
          dstPath = "quality"
          value.path = "M.D.L.q"
        }
      }
      
      i++
      with ( p[i].matchQuery.and ) {
        with( .left.equal ) {
          .path = "year"
          .data = 2020
        }
        with ( .right.and ) {
          with(.left.equal ) {
            .path = "month"
            .data = 11
          }
          with( .right.or ) {
            with( .left.equal ) {
              .path = "day"
              .data = 29
            }
            with( .right.equal ) {
              .path = "day"
              .data = 30
            }
          }
        }
      }
      
      i++
      p[i].groupQuery.aggregate << {
        .dstPath = "quality"
        .srcPath = "quality"
      }
      
      i++
      with ( p[i] ) {
        .projectQuery[0] << {
          .dstPath = "quality"
          .value.path = "quality"
        }
        .projectQuery[1] << {
          .dstPath = "patient_id"
          .value = pseudoId
        }
      }

      i++
      with ( p[i].lookupQuery ) {
        .rightData << biometricResponse.result;
        .leftPath = "patient_id"
        .rightPath = "patient_id"
        .dstPath = "temperatures"
      }
      
      i++
      with ( p[i] ) {
        .projectQuery[0] << {
          dstPath = "quality"
          value.path = "quality"
        }
        .projectQuery[1] << {
          dstPath = "temperatures"
          value.path = "temperatures.t"
        }
        .projectQuery[2] << {
          dstPath = "patient_id",
          value.path = "patient_id"
        }
      }

      pipeline@TQuery( sleeplogRequest )( bsResponse )

      println@Console( "The pipeline test executed succesfully in " 
      + ( biometricRequest.queryTime + bsResponse.queryTime ) + "ms" )()

      // valueToPrettyString@StringUtils( bsResponse )( s )
      // println@Console( s )()

  }
}