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

import jolie.runtime.FaultException;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import joliex.tquery.engine.common.TQueryExpression;
import joliex.tquery.engine.common.Utils;

import java.util.Iterator;
import java.util.LinkedList;

public class ProjectExpressionChain implements ProjectExpression {

	private final LinkedList< ProjectExpression > expressions = new LinkedList<>();

	public ProjectExpressionChain addExpression( ProjectExpression expression ) {
		expressions.add( expression );
		return this;
	}

	@Override
	public Value applyOn( Value element ) throws FaultException {
		if ( expressions.isEmpty() ) {
			return element;
		} else {
			return _applyOn( element, expressions.iterator() );
		}
	}

	private Value _applyOn( Value element, Iterator< ProjectExpression > i ) throws FaultException {
		ProjectExpression pe = i.next();
		if( i.hasNext() ){
			return Utils.merge( pe.applyOn( element ), _applyOn( element, i ) );
		} else {
			return pe.applyOn( element );
		}
	}
}
