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

package joliex.tquery.engine.project.valuedefinition;

import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import joliex.tquery.engine.match.MatchExpression;

public class MatchValueDefinition implements ValueDefinition {

	private final MatchExpression matchExpression;
	
	public MatchValueDefinition( MatchExpression matchExpression ){
		this.matchExpression = matchExpression;
	}
	
	@Override
	public ValueVector evaluate( Value value ) {
		ValueVector returnVector = ValueVector.create();
		returnVector.add( Value.create( matchExpression.applyOn( value ) ) );
		return returnVector;
	}

	@Override
	public boolean isDefined( Value value ) {
		return true;
	}
	
}
