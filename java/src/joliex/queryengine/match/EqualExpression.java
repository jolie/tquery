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

package joliex.queryengine.match;

import java.util.Optional;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import joliex.queryengine.common.Path;
import joliex.queryengine.common.Utils;

public class EqualExpression implements MatchExpression {
	
	private final Path path;
	private final ValueVector vector;
	
	public EqualExpression( Path path, ValueVector vector ){
		this.path = path;
		this.vector = vector;
	}

	@Override
	public boolean[] applyOn( ValueVector elements ) {
		boolean[] mask = MatchUtils.getMask( elements );
		for ( int i = 0; i < mask.length; i++ ) {
			mask[ i ] = applyOn( elements.get( i ) );
		}
		return mask;
	}

	@Override
	public boolean applyOn( Value element ) {
		Optional<ValueVector> pathApplication = path.apply( element );
		return pathApplication.isEmpty() ? false : Utils.checkVectorEquality( pathApplication.get(), vector );
	}
	
}
