/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.model;

import org.neo4j.graphdb.RelationshipType;
import org.structr.core.entity.ManyToMany;

/**
 *
 * @author axel
 */
public class FriendsOfFriends extends ManyToMany<Person, Person> implements RelationshipType {

	@Override
	public Class<Person> getSourceType() {
		return Person.class;
	}

	@Override
	public Class<Person> getTargetType() {
		return Person.class;
	}

	@Override
	public String name() {
		return "FRIEND_OF";
	}

}
