/*******************************************************************************
 *   Copyright (C) 2018 by Larisa Safina <safina@imada.sdu.dk>                 *
 *   Copyright (C) 2018 by Stefano Pio Zingaro <stefanopio.zingaro@unibo.it>   *
 *   Copyright (C) 2018 by Saverio Giallorenzo <saverio.giallorenzo@gmail.com> *
 *                                                                             *
 *   This program is free software; you can redistribute it and/or modify      *
 *   it under the terms of the GNU Library General Public License as           *
 *   published by the Free Software Foundation; either version 2 of the        *
 *   License, or (at your option) any later version.                           *
 *                                                                             *
 *   This program is distributed in the hope that it will be useful,           *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of            *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the             *
 *   GNU General Public License for more details.                              *
 *                                                                             *
 *   You should have received a copy of the GNU Library General Public         * 
 *   License along with this program; if not, write to the                     *
 *   Free Software Foundation, Inc.,                                           *
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.                 *
 *                                                                             *
 *   For details about the authors of this software, see the AUTHORS file.     *
 *******************************************************************************/

package joliex.tquery.engine.pipeline;

import jolie.runtime.FaultException;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import joliex.tquery.engine.LookupService;
import joliex.tquery.engine.MatchService;
import joliex.tquery.engine.common.Path;
import joliex.tquery.engine.common.TQueryExpression;
import joliex.tquery.engine.common.Utils;
import joliex.tquery.engine.group.GroupQuery;
import joliex.tquery.engine.lookup.LookupQuery;
import joliex.tquery.engine.match.MatchQuery;
import joliex.tquery.engine.project.ProjectQuery;
import joliex.tquery.engine.unwind.UnwindQuery;

import java.util.Optional;

public final class PipelineQuery {

	private static class RequestType {

		private static final String DATA = "data";
		private static final String PIPELINE = "pipeline";

		private static class QuerySubtype {

			private static final String MATCH_QUERY = "matchQuery";
			private static final String PROJECT_QUERY = "projectQuery";
			private static final String UNWIND_QUERY = "unwindQuery";
			private static final String GROUP_QUERY = "groupQuery";
			private static final String LOOKUP_QUERY = "lookupQuery";
		}
	}

	private static final String QUERY = "query";

	public static Value pipeline( Value pipelineRequest ) throws FaultException {

		ValueVector pipeline = pipelineRequest.getChildren( RequestType.PIPELINE );
		pipelineRequest.children().remove( "pipeline" );

		for ( int i = 0; i < pipeline.size(); i++ ) {
			Value stage = pipeline.get( i );

			if ( stage.hasChildren( RequestType.QuerySubtype.MATCH_QUERY ) ) {

				pipelineRequest.children().put( QUERY, stage.getChildren( RequestType.QuerySubtype.MATCH_QUERY ) );
				Value response = MatchQuery.match( pipelineRequest );
				pipelineRequest.children().put( RequestType.DATA, response.getChildren( TQueryExpression.ResponseType.RESULT ) );

			} else  if ( stage.hasChildren( RequestType.QuerySubtype.PROJECT_QUERY ) ) {

				pipelineRequest.children().put( QUERY, stage.getChildren( RequestType.QuerySubtype.PROJECT_QUERY ) );
				Value response = ProjectQuery.project( pipelineRequest );
				pipelineRequest.children().put( RequestType.DATA, response.getChildren( TQueryExpression.ResponseType.RESULT ) );

			} else  if ( stage.hasChildren( RequestType.QuerySubtype.UNWIND_QUERY ) ) {

				pipelineRequest.children().put( QUERY, stage.getChildren( RequestType.QuerySubtype.UNWIND_QUERY ) );
				Value response = UnwindQuery.unwind( pipelineRequest );
				pipelineRequest.children().put( RequestType.DATA, response.getChildren( TQueryExpression.ResponseType.RESULT ) );

			} else if ( stage.hasChildren( RequestType.QuerySubtype.GROUP_QUERY) ) {

				pipelineRequest.children().put( QUERY, stage.getChildren( RequestType.QuerySubtype.GROUP_QUERY ) );
				Value response = GroupQuery.group( pipelineRequest );
				pipelineRequest.children().put( RequestType.DATA, response.getChildren( TQueryExpression.ResponseType.RESULT ) );

			} else if ( stage.hasChildren( RequestType.QuerySubtype.LOOKUP_QUERY ) ) {

				pipelineRequest.children().put( "leftData", pipelineRequest.getChildren( RequestType.DATA ) );
				pipelineRequest.children().remove( QUERY );
				pipelineRequest.children().remove( RequestType.DATA );
				Value lookupQuery = stage.getFirstChild( RequestType.QuerySubtype.LOOKUP_QUERY );
				pipelineRequest.children().put( "leftPath", lookupQuery.getChildren( "leftPath" ) );
				pipelineRequest.children().put( "rightData", lookupQuery.getChildren( "rightData" ) );
				pipelineRequest.children().put( "rightPath", lookupQuery.getChildren( "rightPath" ) );
				pipelineRequest.children().put( "dstPath", lookupQuery.getChildren( "dstPath" ) );

				Value response = LookupQuery.lookup( pipelineRequest );
				pipelineRequest.children().remove( "leftData" );
				pipelineRequest.children().remove( "leftPath" );
				pipelineRequest.children().remove( "rightData" );
				pipelineRequest.children().remove( "rightPath" );
				pipelineRequest.children().remove( "dstPath" );
				pipelineRequest.children().put( RequestType.DATA, response.getChildren( TQueryExpression.ResponseType.RESULT ) );

			} else {

				throw new FaultException( "Unrecognized operation at position " + i );

			}

		}

		Value response = Value.create();
		response.children().put( TQueryExpression.ResponseType.RESULT, pipelineRequest.getChildren( RequestType.DATA ) );

		return response;
	}

}
