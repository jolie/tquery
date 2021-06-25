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

package joliex.tquery.engine.project;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import jolie.runtime.FaultException;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import joliex.tquery.engine.common.Path;
import joliex.tquery.engine.common.TQueryExpression;

import java.util.Optional;

public class PathProjectExpression implements ProjectExpression {

	private final Path path;

	public PathProjectExpression( String path ) throws FaultException {
		this.path = Path.parsePath( path );
	}

	private PathProjectExpression( Path path ) {
		this.path = path;
	}

	@Override
	public Value applyOn( Value element ) throws FaultException {
		Value returnValue = Value.create();
		Optional< ValueVector > valueVector = Path.parsePath( path.getCurrentNode() ).apply( element );
		if ( valueVector.isPresent() ) {
			returnValue.children().put( path.getCurrentNode(), valueVector.get() );
			if ( path.getContinuation().isPresent() ) {
				returnValue = new PathProjectExpression( path.getContinuation().get() ).applyOn( returnValue );
			}
		}
		return returnValue;
	}

}
